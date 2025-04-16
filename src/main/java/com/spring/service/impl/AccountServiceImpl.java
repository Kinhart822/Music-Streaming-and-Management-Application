package com.spring.service.impl;

import com.spring.constants.*;
import com.spring.dto.request.account.*;
import com.spring.dto.response.*;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final JwtHelper jwtHelper;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final EmailService emailService;
    private static final String EMAIL_EXISTENCE_KEY = "emailExisted";
    private static final String OTP_DUE_DATE_KEY = "otpDueDate";
    private static final String VALID_KEY = "isValid";
    private static final RandomStringGenerator numericGenerator = new RandomStringGenerator.Builder().withinRange('0', '9').build();
    private static final Map<String, Otp> sessionOtpMap = new HashMap<>();
    private final NotificationRepository notificationRepository;
    private final CloudinaryService cloudinaryService;

    // For both Artist and Admin
    @Override
    public ApiResponse createAdmin(CreateAdmin request) {
        String email = request.getEmail();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ApiResponseCode.USERNAME_EXISTED);
        }

        Long creatorId = jwtHelper.getIdUserRequesting();
        Instant now = Instant.now();

        User admin = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.ADMIN)
                .status(CommonStatus.ACTIVE.getStatus())
                .createdBy(creatorId)
                .lastModifiedBy(creatorId)
                .createdDate(now)
                .lastModifiedDate(now)
                .build();

        userRepository.save(admin);

        return ApiResponse.ok();
    }

    @Override
    public ApiResponse createArtist(CreateArtist request) {
        String email = request.getEmail();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ApiResponseCode.USERNAME_EXISTED);
        }

        Long creatorId = jwtHelper.getIdUserRequesting();
        Instant now = Instant.now();

        Artist artist = new Artist();
        artist.setEmail(email);
        artist.setPassword(passwordEncoder.encode(request.getPassword()));
        artist.setUserType(UserType.ARTIST);
        artist.setStatus(CommonStatus.ACTIVE.getStatus());
        artist.setCreatedDate(now);
        artist.setLastModifiedDate(now);
        artist.setDescription(null);
        artist.setImageUrl(null);
        artist.setCountListen(0L);
        artist.setCreatedBy(creatorId);
        artist.setLastModifiedBy(creatorId);
        artistRepository.save(artist);

        return ApiResponse.ok();
    }

    @Override
    public ApiResponse createArtistFromList(CreateArtistFromList request) {
        List<CreateArtist> artistRequests = request.getArtistList();

        if (artistRequests == null || artistRequests.isEmpty()) {
            throw new BusinessException(ApiResponseCode.INVALID_HTTP_REQUEST);
        }

        Long creatorId = jwtHelper.getIdUserRequesting();
        Instant now = Instant.now();

        List<Artist> artistsInfoToSave = new ArrayList<>();
        int skipped = 0;

        for (CreateArtist artistRequest : artistRequests) {
            String email = artistRequest.getEmail();
            if (userRepository.existsByEmailIgnoreCase(email)) {
                skipped++;
                continue;
            }

            // Tạo Artist (kế thừa từ User)
            Artist artist = new Artist();
            artist.setEmail(email);
            artist.setPassword(passwordEncoder.encode(artistRequest.getPassword()));
            artist.setUserType(UserType.ARTIST);
            artist.setStatus(CommonStatus.ACTIVE.getStatus());
            artist.setCreatedDate(now);
            artist.setLastModifiedDate(now);
            artist.setDescription(null);
            artist.setImageUrl(null);
            artist.setCountListen(0L);
            artist.setCreatedBy(creatorId);
            artist.setLastModifiedBy(creatorId);
            artistRepository.save(artist);

            artistsInfoToSave.add(artist);
        }

        return ApiResponse.ok(
                "Bulk Artist Creation Result",
                "Created " + artistsInfoToSave.size() + " artist(s), skipped " + skipped + " duplicate(s)!"
        );
    }

    // User
    @Override
    public Map<String, Instant> sendOtpToEmail(SendOtpRequest request) {
        String email = request.getEmail();
        String sessionId = request.getSessionId();
        sessionOtpMap.computeIfAbsent(sessionId, o -> Otp.builder()
                .email(email)
                .otp(numericGenerator.generate(6))
                .dueDate(Instant.now().plusSeconds(60))
                .build());
        Otp otp = sessionOtpMap.get(sessionId);
        emailService.sendEmailVerificationOtp(otp);
        Map<String, Instant> response = new HashMap<>();
        response.put(OTP_DUE_DATE_KEY, otp.getDueDate());
        return response;
    }

    @Override
    public Map<String, Boolean> checkOtp(CheckOtpRequest request) {
        String sessionId = request.getSessionId();
        if (!sessionOtpMap.containsKey(sessionId)) {
            throw new BusinessException(ApiResponseCode.SESSION_ID_NOT_FOUND);
        }
        Otp otp = sessionOtpMap.get(sessionId);
        Map<String, Boolean> response = new HashMap<>();
        response.put(VALID_KEY, Objects.equals(otp.getOtp(), request.getOtp()) && otp.getDueDate().isAfter(Instant.now()));
        return response;
    }

    @Override
    public List<Notification> findAllNotifications() {
        Long userId = jwtHelper.getIdUserRequesting();
        return notificationRepository.findAllByUserOrderByCreatedDateDesc(userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND.setDescription(String.format("User not found with id: %d", userId)))));
    }

    @Override
    public Long countArtists() {
        return artistRepository.countAllArtists();
    }

    @Override
    public Long countUsers() {
        return userRepository.countAllUsers();
    }

    @Override
    public Long countSongs() {
        return songRepository.countAllSongs();
    }

    @Override
    public AdminPresentation getAdmin() {
        User admin = userRepository.findById(jwtHelper.getIdUserRequesting())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = admin.getBirthDay() != null ?
                admin.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        return AdminPresentation.builder()
                .id(admin.getId())
                .avatar(admin.getAvatar() != null ? admin.getAvatar() : "")
                .firstName(admin.getFirstName() != null ? admin.getFirstName() : "")
                .lastName(admin.getLastName() != null ? admin.getLastName() : "")
                .email(admin.getEmail())
                .gender(admin.getGender() != null ? admin.getGender().toString() : "")
                .birthDay(formattedDateOfBirth)
                .phone(admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "")
                .status(admin.getStatus())
                .createdBy(admin.getCreatedBy())
                .lastModifiedBy(admin.getLastModifiedBy())
                .createdDate(admin.getCreatedDate())
                .lastModifiedDate(admin.getLastModifiedDate())
                .build();
    }

    @Override
    public UserPresentation getUser() {
        User user = userRepository.findById(jwtHelper.getIdUserRequesting())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = user.getBirthDay() != null ?
                user.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        return UserPresentation.builder()
                .id(user.getId())
                .avatar(user.getAvatar() != null ? user.getAvatar() : "")
                .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                .lastName(user.getLastName() != null ? user.getLastName() : "")
                .email(user.getEmail())
                .gender(user.getGender() != null ? user.getGender().toString() : "")
                .birthDay(formattedDateOfBirth)
                .phone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
                .status(user.getStatus())
                .createdBy(user.getCreatedBy())
                .lastModifiedBy(user.getLastModifiedBy())
                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .build();
    }

    @Override
    public UserPresentation viewUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = user.getBirthDay() != null ?
                user.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        return UserPresentation.builder()
                .id(user.getId())
                .avatar(user.getAvatar() != null ? user.getAvatar() : "")
                .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                .lastName(user.getLastName() != null ? user.getLastName() : "")
                .email(user.getEmail())
                .gender(user.getGender() != null ? user.getGender().toString() : "")
                .birthDay(formattedDateOfBirth)
                .phone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
                .status(user.getStatus())
                .createdBy(user.getCreatedBy())
                .lastModifiedBy(user.getLastModifiedBy())
                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .build();
    }

    @Override
    public ArtistPresentation getArtist() {
        User artist = userRepository.findById(jwtHelper.getIdUserRequesting())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        Artist artist_info = artistRepository.findById(artist.getId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = artist.getBirthDay() != null ?
                artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        return ArtistPresentation.builder()
                .id(artist.getId())
                .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                .description(artist_info.getDescription() != null ? artist_info.getDescription() : "")
                .image(artist_info.getImageUrl() != null ? artist_info.getImageUrl() : "")
                .countListen(artist_info.getCountListen() != null ? artist_info.getCountListen() : 0)
                .email(artist.getEmail())
                .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                .birthDay(formattedDateOfBirth)
                .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                .status(artist.getStatus())
                .createdBy(artist.getCreatedBy())
                .lastModifiedBy(artist.getLastModifiedBy())
                .createdDate(artist.getCreatedDate())
                .lastModifiedDate(artist.getLastModifiedDate())
                .build();
    }

    @Override
    public ArtistPresentation viewArtistProfile(Long id) {
        User artist = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        Artist artist_info = artistRepository.findById(artist.getId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = artist.getBirthDay() != null ?
                artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        return ArtistPresentation.builder()
                .id(artist.getId())
                .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                .description(artist_info.getDescription() != null ? artist_info.getDescription() : "")
                .image(artist_info.getImageUrl() != null ? artist_info.getImageUrl() : "")
                .countListen(artist_info.getCountListen() != null ? artist_info.getCountListen() : 0)
                .email(artist.getEmail())
                .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                .birthDay(formattedDateOfBirth)
                .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                .status(artist.getStatus())
                .createdBy(artist.getCreatedBy())
                .lastModifiedBy(artist.getLastModifiedBy())
                .createdDate(artist.getCreatedDate())
                .lastModifiedDate(artist.getLastModifiedDate())
                .build();
    }

    @Override
    public List<AdminPresentation> getAllAdmin() {
        List<User> admins = userRepository.findByUserType(UserType.ADMIN);

        return admins.stream().map(admin -> {
            String formattedDate = admin.getBirthDay() != null
                    ? admin.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

            return AdminPresentation.builder()
                    .id(admin.getId())
                    .avatar(admin.getAvatar() != null ? admin.getAvatar() : "")
                    .firstName(admin.getFirstName() != null ? admin.getFirstName() : "")
                    .lastName(admin.getLastName() != null ? admin.getLastName() : "")
                    .email(admin.getEmail())
                    .gender(admin.getGender() != null ? admin.getGender().toString() : "")
                    .birthDay(formattedDate)
                    .phone(admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "")
                    .status(admin.getStatus())
                    .createdBy(admin.getCreatedBy())
                    .lastModifiedBy(admin.getLastModifiedBy())
                    .createdDate(admin.getCreatedDate())
                    .lastModifiedDate(admin.getLastModifiedDate())
                    .build();
        }).toList();
    }

    @Override
    public List<UserPresentation> getAllUser() {
        List<User> users = userRepository.findByUserType(UserType.USER);

        return users.stream().map(user -> {
            String formattedDate = user.getBirthDay() != null
                    ? user.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

            return UserPresentation.builder()
                    .id(user.getId())
                    .avatar(user.getAvatar() != null ? user.getAvatar() : "")
                    .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                    .lastName(user.getLastName() != null ? user.getLastName() : "")
                    .email(user.getEmail())
                    .gender(user.getGender() != null ? user.getGender().toString() : "")
                    .birthDay(formattedDate)
                    .phone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
                    .status(user.getStatus())
                    .createdBy(user.getCreatedBy())
                    .lastModifiedBy(user.getLastModifiedBy())
                    .createdDate(user.getCreatedDate())
                    .lastModifiedDate(user.getLastModifiedDate())
                    .build();
        }).toList();
    }

    @Override
    public List<ArtistPresentation> getAllArtist() {
        List<Artist> artists = artistRepository.findAll();

        return artists.stream().map(artist -> {
            String formattedDate = artist.getBirthDay() != null
                    ? artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

            return ArtistPresentation.builder()
                    .id(artist.getId())
                    .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                    .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                    .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                    .email(artist.getEmail())
                    .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                    .birthDay(formattedDate)
                    .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                    .status(artist.getStatus())
                    .createdBy(artist.getCreatedBy())
                    .lastModifiedBy(artist.getLastModifiedBy())
                    .createdDate(artist.getCreatedDate())
                    .lastModifiedDate(artist.getLastModifiedDate())
                    .description(artist.getDescription() != null ? artist.getDescription() : "")
                    .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                    .countListen(artist.getCountListen() != null ? artist.getCountListen() : 0)
                    .build();
        }).toList();
    }


    // Sign Up Request
    @Override
    public Map<String, Boolean> signUpCheckEmailExistence(String query) {
        Map<String, Boolean> response = new HashMap<>();
        response.put(EMAIL_EXISTENCE_KEY, userRepository.existsByEmailIgnoreCase(query));
        return response;
    }

    @Override
    public Map<String, Instant> signUpBegin(SendOtpRequest request) {
        return sendOtpToEmail(request);
    }

    @Override
    public Map<String, Boolean> signUpCheckOtp(CheckOtpRequest request) {
        return checkOtp(request);
    }

    @Override
    public ApiResponse signUpFinish(SignUpRequest request) {
        String sessionId = request.getSessionId();
        if (!sessionOtpMap.containsKey(sessionId)) {
            throw new BusinessException(ApiResponseCode.SESSION_ID_NOT_FOUND);
        }
        Instant now = Instant.now();
        User user = User.builder()
                .email(sessionOtpMap.get(sessionId).getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.USER)
                .status(CommonStatus.ACTIVE.getStatus())
                .createdDate(now)
                .lastModifiedDate(now)
                .build();

        user = userRepository.save(user);

        user.setCreatedBy(user.getId());
        user.setLastModifiedBy(user.getId());

        userRepository.save(user);

        sessionOtpMap.remove(sessionId);
        return ApiResponse.ok();
    }

    // Forgot Password Request
    @Override
    public Map<String, Instant> forgotPasswordBegin(SendOtpRequest request) {
        return sendOtpToEmail(request);
    }

    @Override
    public Map<String, Boolean> forgotPasswordCheckOtp(CheckOtpRequest request) {
        return checkOtp(request);
    }

    @Override
    public ApiResponse forgotPasswordFinish(ForgotPasswordFinish request) {
        String sessionId = request.getSessionId();
        if (!sessionOtpMap.containsKey(sessionId)) {
            throw new BusinessException(ApiResponseCode.SESSION_ID_NOT_FOUND);
        }
        userRepository
                .save(((User) userService
                        .loadUserByUsername(sessionOtpMap.get(sessionId).getEmail()))
                        .toBuilder()
                        .password(passwordEncoder.encode(request.getPassword()))
                        .lastModifiedDate(Instant.now())
                        .build());
        sessionOtpMap.remove(sessionId);
        return ApiResponse.ok();
    }

    // Reset Password Request
    @Override
    public ApiResponse resetPasswordRequest(ResetPasswordRequest request) {
        Optional<User> user = userService.resetPasswordRequest(request.getEmail());
        if (user.isEmpty()) {
            throw new BusinessException(ApiResponseCode.USERNAME_NOT_EXISTED_OR_DEACTIVATED);
        }
        userRepository.save(user.get());
        emailService.sendResetPasswordMail(user.get());
        return ApiResponse.ok();
    }

    @Override
    public Map<String, Boolean> resetPasswordCheck(ResetPasswordCheck check) {
        String resetKey = check.getResetKey();

        Optional<User> user = userService.resetPasswordCheck(resetKey);
        Map<String, Boolean> response = new HashMap<>();
        response.put(VALID_KEY, user.isPresent());
        return response;
    }

    @Override
    public ApiResponse resetPasswordFinish(ResetPasswordFinish finish) {
        Optional<User> user = userService.resetPasswordFinish(finish.getResetKey(), passwordEncoder.encode(finish.getNewPassword()));
        if (user.isEmpty()) {
            throw new BusinessException(ApiResponseCode.INVALID_RESET_KEY);
        }
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse updateAccount(UpdateAccountRequest request) {
        Long id = jwtHelper.getIdUserRequesting();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            String imageUrl = cloudinaryService.uploadImageToCloudinary(request.getAvatar());
            user.setAvatar(imageUrl);
        }

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }

        if (request.getGender() != null && !request.getGender().name().isBlank()) {
            user.setGender(request.getGender());
        }

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate dateOfBirth = LocalDate.parse(request.getDateOfBirth(), formatter);
            user.setBirthDay(dateOfBirth);
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhoneNumber(request.getPhone());
        }

        user.setLastModifiedBy(id);
        user.setLastModifiedDate(Instant.now());
        userRepository.save(user);

        return ApiResponse.ok();
    }

    @Override
    @Transactional
    public ApiResponse deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        switch (user.getUserType()) {
            case USER:
                handleUserDeletion(user);
                break;

            case ARTIST:
                handleArtistDeletion(user);
                break;

            case ADMIN:
                handleAdminDeactivation(user);
                break;

            default:
                throw new BusinessException(ApiResponseCode.INVALID_TYPE);
        }

        return ApiResponse.ok();
    }

    private void handleUserDeletion(User user) {
        user.setStatus(CommonStatus.DELETED.getStatus());
        user.setLastModifiedBy(user.getId());
        user.setLastModifiedDate(Instant.now());
        userRepository.save(user); // Chỉ cập nhật trạng thái
    }

    @Scheduled(fixedRate = 60000) // Chạy mỗi 1 phút
    public void deleteUsersMarkedAsDeleted() {
        Instant tenMinutesAgo = Instant.now().minus(Duration.ofMinutes(10));
        List<User> usersToDelete = userRepository.findAllByStatusAndLastModifiedDateBefore(
                CommonStatus.DELETED.getStatus(), tenMinutesAgo
        );

        // Hibernate tự xóa cả liên quan nếu cascade đúng
        userRepository.deleteAll(usersToDelete);
    }

    private void handleArtistDeletion(User user) {
        user.setStatus(CommonStatus.INACTIVE.getStatus());
        user.setLastModifiedBy(user.getId());
        user.setLastModifiedDate(Instant.now());
        userRepository.save(user);
    }

    private void handleAdminDeactivation(User user) {
        user.setStatus(CommonStatus.LOCKED.getStatus());
        user.setLastModifiedBy(user.getId());
        user.setLastModifiedDate(Instant.now());
        userRepository.save(user);
    }

    @Override
    public List<AdminPresentation> getAllAdminByLockedStatus() {
        // Lọc người dùng với status LOCKED (-4)
        List<User> lockedAdmins = userRepository.findAllByStatus(CommonStatus.LOCKED.getStatus());

        // Chuyển thành AdminPresentation
        return lockedAdmins.stream()
                .map(admin -> {
                    String formattedDate = admin.getBirthDay() != null
                            ? admin.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

                    return AdminPresentation.builder()
                            .id(admin.getId())
                            .avatar(admin.getAvatar() != null ? admin.getAvatar() : "")
                            .firstName(admin.getFirstName() != null ? admin.getFirstName() : "")
                            .lastName(admin.getLastName() != null ? admin.getLastName() : "")
                            .email(admin.getEmail())
                            .gender(admin.getGender() != null ? admin.getGender().toString() : "")
                            .birthDay(formattedDate)
                            .phone(admin.getPhoneNumber() != null ? admin.getPhoneNumber() : "")
                            .status(admin.getStatus())
                            .createdBy(admin.getCreatedBy())
                            .lastModifiedBy(admin.getLastModifiedBy())
                            .createdDate(admin.getCreatedDate())
                            .lastModifiedDate(admin.getLastModifiedDate())
                            .build();
                }).toList();
    }

    @Override
    public List<ArtistPresentation> getAllArtistByInactiveStatus() {
        // Lọc người dùng với status INACTIVE (-1)
        List<Artist> inactiveArtists = artistRepository.findAllByStatus(CommonStatus.INACTIVE.getStatus());

        return inactiveArtists.stream().map(artist -> {
            String formattedDate = artist.getBirthDay() != null
                    ? artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

            return ArtistPresentation.builder()
                    .id(artist.getId())
                    .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                    .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                    .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                    .email(artist.getEmail())
                    .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                    .birthDay(formattedDate)
                    .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                    .status(artist.getStatus())
                    .createdBy(artist.getCreatedBy())
                    .lastModifiedBy(artist.getLastModifiedBy())
                    .createdDate(artist.getCreatedDate())
                    .lastModifiedDate(artist.getLastModifiedDate())
                    .description(artist.getDescription() != null ? artist.getDescription() : "")
                    .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                    .countListen(artist.getCountListen() != null ? artist.getCountListen() : 0)
                    .build();
        }).toList();
    }

    @Override
    public ApiResponse processingDeleteRequest(Long userId, String manageFunction) {
        Long userModifyId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (user.getUserType() == UserType.USER) {
            throw new BusinessException(ApiResponseCode.INVALID_TYPE);
        }

        int currentStatus = user.getStatus();
        boolean isArtist = user.getUserType() == UserType.ARTIST;
        boolean validToDelete = (isArtist && currentStatus == CommonStatus.INACTIVE.getStatus()) ||
                (!isArtist && currentStatus == CommonStatus.LOCKED.getStatus());

        if (!validToDelete) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        if (manageFunction.equalsIgnoreCase(ManageProcess.DELETED.name())) {
            user.setLastModifiedBy(userModifyId);
            user.setLastModifiedDate(Instant.now());
            user.setStatus(CommonStatus.DELETED.getStatus());
        } else {
            user.setLastModifiedBy(userModifyId);
            user.setLastModifiedDate(Instant.now());
            user.setStatus(CommonStatus.ACTIVE.getStatus());
        }

        user.setLastModifiedDate(Instant.now());
        userRepository.save(user);

        String message = (user.getStatus() == CommonStatus.DELETED.getStatus())
                ? "Account deleted successfully!"
                : "Account revoked successfully!";
        return ApiResponse.ok(message);
    }
}
