package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.constants.CommonStatus;
import com.spring.constants.UserType;
import com.spring.dto.request.account.*;
import com.spring.dto.response.*;
import com.spring.entities.*;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HistoryListenRepository historyListenRepository;
    private final UserFileRepository userFileRepository;
    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;
    private final PlayListRepository playListRepository;
    private final PlayListSongRepository playListSongRepository;
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

    // For both Artist and Admin
    @Override
    public ApiResponse createAdmin(CreateAdmin request) {
        String email = request.getEmail();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ApiResponseCode.USERNAME_EXISTED);
        }
        Long userId = jwtHelper.getIdUserRequesting();
        Instant now = Instant.now();
        userRepository
                .save(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(request.getPassword()))
                        .userType(UserType.ADMIN)
                        .status(CommonStatus.ACTIVE.getStatus())
                        .createdBy(userId)
                        .createdDate(now)
                        .lastModifiedBy(userId)
                        .lastModifiedDate(now)
                        .build());
        return ApiResponse.ok();
    }

    @Override
    public ApiResponse createArtist(CreateArtist request) {
        String email = request.getEmail();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ApiResponseCode.USERNAME_EXISTED);
        }

        Long userId = jwtHelper.getIdUserRequesting();
        Instant now = Instant.now();

        // Tạo User
        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.ARTIST)
                .status(CommonStatus.ACTIVE.getStatus())
                .createdBy(userId)
                .createdDate(now)
                .lastModifiedBy(userId)
                .lastModifiedDate(now)
                .build());

        // Tạo Artist liên kết với User vừa tạo
        Artist artist = new Artist();
        artist.setId(user.getId());
        artist.setDescription(null);
        artist.setImage(null);
        artist.setCountListen(0L);

        // Lưu Artist vào cơ sở dữ liệu
        artistRepository.save(artist);

        return ApiResponse.ok();
    }

    @Override
    public ApiResponse createArtistFromList(CreateArtistFromList request) {
        List<CreateArtist> artistRequests = request.getArtistList();

        if (artistRequests == null || artistRequests.isEmpty()) {
            throw new BusinessException(ApiResponseCode.INVALID_HTTP_REQUEST);
        }

        Long userId = jwtHelper.getIdUserRequesting();
        Instant now = Instant.now();

        List<User> artistsToSave = new ArrayList<>();
        int skipped = 0;

        for (CreateArtist artistRequest : artistRequests) {
            String email = artistRequest.getEmail();
            if (userRepository.existsByEmailIgnoreCase(email)) {
                log.warn("Email {} already exists. Skipping artist creation.", email);
                skipped++;
                continue;
            }

            User artist = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(artistRequest.getPassword()))
                    .userType(UserType.ARTIST)
                    .status(CommonStatus.ACTIVE.getStatus())
                    .createdBy(userId)
                    .createdDate(now)
                    .lastModifiedBy(userId)
                    .lastModifiedDate(now)
                    .build();

            artistsToSave.add(artist);
        }

        if (!artistsToSave.isEmpty()) {
            userRepository.saveAll(artistsToSave);
        }

        return ApiResponse.ok(
                "Bulk Artist Creation Result",
                "Created " + artistsToSave.size() + " artist(s), skipped " + skipped + " duplicate(s)."
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
    public AdminPresentation getAdmin() {
        User admin = userRepository
                .findById(jwtHelper.getIdUserRequesting())
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
        User user = userRepository
                .findById(jwtHelper.getIdUserRequesting())
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
        User artist = userRepository
                .findById(jwtHelper.getIdUserRequesting())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        Artist artist_info = artistRepository
                .findById(artist.getId())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = artist.getBirthDay() != null ?
                artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        return ArtistPresentation.builder()
                .id(artist.getId())
                .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                .description(artist_info.getDescription() != null ? artist_info.getDescription() : "")
                .image(artist_info.getImage() != null ? artist_info.getImage() : "")
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate dateOfBirth = LocalDate.parse(request.getDateOfBirth(), formatter);

        userRepository
                .save(userRepository
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND))
                        .toBuilder()
                        .avatar(request.getAvatar())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .gender(request.getGender())
                        .birthDay(dateOfBirth)
                        .phoneNumber(request.getPhone())
                        .lastModifiedBy(id)
                        .lastModifiedDate(Instant.now())
                        .build());
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
        refreshTokenRepository.deleteAllByUser(user);
        notificationRepository.deleteAllByUser(user);
        historyListenRepository.deleteAllByUser(user);
        userFileRepository.deleteAllByUser(user);

        for (Playlist playlist : user.getPlaylists()) {
            playListSongRepository.deleteByPlaylistId(playlist.getId());
            playListRepository.deleteById(playlist.getId());
        }

        user.setStatus(CommonStatus.DELETED.getStatus());
        user.setLastModifiedDate(Instant.now());
        userRepository.delete(user);

        log.info("User [{}] deleted permanently.", user.getId());
    }

    private void handleArtistDeletion(User user) {
        List<Song> songs = songRepository.findByArtist(user);
        for (Song song : songs) {
            song.setDownloadPermission(false);
            song.setCountListen(0L);
            songRepository.save(song);
        }

        List<Album> albums = albumRepository.findByArtist(user);
        for (Album album : albums) {
            album.setDownloadPermission(false);
            album.setCountListen(0L);
            album.setTotalListen(0);
            albumRepository.save(album);
        }

        user.setStatus(CommonStatus.INACTIVE.getStatus());
        user.setLastModifiedDate(Instant.now());
        userRepository.save(user);

        log.info("Artist [{}] marked inactive. Songs/Albums hidden from client.", user.getId());
    }

    private void handleAdminDeactivation(User user) {
        user.setStatus(CommonStatus.LOCKED.getStatus());
        user.setLastModifiedDate(Instant.now());
        userRepository.save(user);

        log.warn("Admin [{}] marked as LOCKED. Admin deletion is not permitted.", user.getId());
    }
}
