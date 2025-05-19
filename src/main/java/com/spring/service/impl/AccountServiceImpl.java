package com.spring.service.impl;

import com.spring.constants.ApiResponseCode;
import com.spring.constants.CommonStatus;
import com.spring.constants.ManageProcess;
import com.spring.constants.UserType;
import com.spring.dto.request.account.*;
import com.spring.dto.response.*;
import com.spring.entities.Artist;
import com.spring.entities.Notification;
import com.spring.entities.User;
import com.spring.exceptions.BusinessException;
import com.spring.repository.*;
import com.spring.security.JwtHelper;
import com.spring.service.AccountService;
import com.spring.service.CloudinaryService;
import com.spring.service.EmailService;
import com.spring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final UserSongCountRepository userSongCountRepository;
    private final ArtistUserFollowRepository artistUserFollowRepository;
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

    // For User, Artist and Admin
    @Override
    public ApiResponse createUser(CreateUser request) {
        String email = request.getEmail();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ApiResponseCode.USERNAME_EXISTED);
        }

        Long creatorId = jwtHelper.getIdUserRequesting();
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.ADMIN)
                .status(CommonStatus.ACTIVE.getStatus())
                .createdBy(creatorId)
                .lastModifiedBy(creatorId)
                .createdDate(now)
                .lastModifiedDate(now)
                .build();

        userRepository.save(user);

        return ApiResponse.ok();
    }

    @Override
    public ApiResponse createAdmin(CreateAdmin request) {
        String email = request.getEmail();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ApiResponseCode.USERNAME_EXISTED);
        }

        Long creatorId = jwtHelper.getIdUserRequesting();
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

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
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Artist artist = new Artist();
        artist.setEmail(email);
        artist.setPassword(passwordEncoder.encode(request.getPassword()));
        artist.setUserType(UserType.ARTIST);
        artist.setStatus(CommonStatus.ACTIVE.getStatus());
        artist.setCreatedDate(now);
        artist.setLastModifiedDate(now);
        artist.setDescription(null);
        artist.setImageUrl(null);
        artist.setCreatedBy(creatorId);
        artist.setLastModifiedBy(creatorId);
        artistRepository.save(artist);

        return ApiResponse.ok();
    }

    @Override
    public ApiResponse signUpArtist(CreateArtist request) {
        String email = request.getEmail();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ApiResponseCode.USERNAME_EXISTED);
        }

        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Artist artist = new Artist();
        artist.setEmail(email);
        artist.setPassword(passwordEncoder.encode(request.getPassword()));
        artist.setUserType(UserType.ARTIST);
        artist.setStatus(CommonStatus.ACTIVE.getStatus());
        artist.setCreatedDate(now);
        artist.setLastModifiedDate(now);
        artist.setDescription(null);
        artist.setImageUrl(null);
        artist.setCreatedBy(0L);
        artist.setLastModifiedBy(0L);
        artistRepository.save(artist);

        artist.setCreatedBy(artist.getId());
        artist.setLastModifiedBy(artist.getId());
        artistRepository.save(artist);

        return ApiResponse.ok();
    }

    // User
    @Override
    public Map<String, ZonedDateTime> sendOtpToEmail(SendOtpRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(300);

        String email = request.getEmail();
        String sessionId = request.getSessionId();
        sessionOtpMap.computeIfAbsent(sessionId, o -> Otp.builder()
                .email(email)
                .otp(numericGenerator.generate(6))
                .dueDate(dueDateInVietnam)
                .build());
        Otp otp = sessionOtpMap.get(sessionId);
        emailService.sendEmailVerificationOtp(otp);
        Map<String, ZonedDateTime> response = new HashMap<>();
        response.put(OTP_DUE_DATE_KEY, dueDateInVietnam);
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
        response.put(VALID_KEY, Objects.equals(otp.getOtp(), request.getOtp()));
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
    public Long countPendingSongs() {
        return songRepository.countAllPendingSongs();
    }

    @Override
    public AdminPresentation getAdmin() {
        User admin = userRepository.findById(jwtHelper.getIdUserRequesting())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = admin.getBirthDay() != null ?
                admin.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedCreatedDate = admin.getCreatedDate() != null
                ? formatter.format(admin.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;
        String formattedLastModifiedDate = admin.getLastModifiedDate() != null
                ? formatter.format(admin.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

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
                .createdDate(formattedCreatedDate)
                .lastModifiedDate(formattedLastModifiedDate)
                .build();
    }

    @Override
    public UserPresentation getUser() {
        User user = userRepository.findById(jwtHelper.getIdUserRequesting())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = user.getBirthDay() != null ?
                user.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedCreatedDate = user.getCreatedDate() != null
                ? formatter.format(user.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;
        String formattedLastModifiedDate = user.getLastModifiedDate() != null
                ? formatter.format(user.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

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
                .createdDate(formattedCreatedDate)
                .lastModifiedDate(formattedLastModifiedDate)
                .build();
    }

    @Override
    public UserPresentation viewUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = user.getBirthDay() != null ?
                user.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedCreatedDate = user.getCreatedDate() != null
                ? formatter.format(user.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;
        String formattedLastModifiedDate = user.getLastModifiedDate() != null
                ? formatter.format(user.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

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
                .createdDate(formattedCreatedDate)
                .lastModifiedDate(formattedLastModifiedDate)
                .build();
    }

    @Override
    public ArtistPresentation getArtist() {
        Artist artist = artistRepository.findById(jwtHelper.getIdUserRequesting())
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = artist.getBirthDay() != null ?
                artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedCreatedDate = artist.getCreatedDate() != null
                ? formatter.format(artist.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;
        String formattedLastModifiedDate = artist.getLastModifiedDate() != null
                ? formatter.format(artist.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

        return ArtistPresentation.builder()
                .id(artist.getId())
                .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                .artistName(artist.getArtistName() != null ? artist.getArtistName() : "")
                .email(artist.getEmail())
                .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                .birthDay(formattedDateOfBirth)
                .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                .status(artist.getStatus())
                .createdBy(artist.getCreatedBy())
                .lastModifiedBy(artist.getLastModifiedBy())
                .createdDate(formattedCreatedDate)
                .lastModifiedDate(formattedLastModifiedDate)
                .description(artist.getDescription() != null ? artist.getDescription() : "")
                .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                .userType(artist.getUserType())
                .artistSongIds(
                        artist.getArtistSongs().stream()
                                .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistSongId().getSong().getId())
                                .collect(Collectors.toList())
                )
                .artistSongNameList(
                        artist.getArtistSongs().stream()
                                .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistSongId().getSong().getTitle())
                                .collect(Collectors.toList())
                )
                .artistPlaylistIds(
                        artist.getArtistPlaylists().stream()
                                .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getId())
                                .collect(Collectors.toList())
                )
                .artistPlaylistNameList(
                        artist.getArtistPlaylists().stream()
                                .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getPlaylistName())
                                .collect(Collectors.toList())
                )
                .artistAlbumIds(
                        artist.getArtistAlbums().stream()
                                .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getId())
                                .collect(Collectors.toList())
                )
                .artistAlbumNameList(
                        artist.getArtistAlbums().stream()
                                .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getAlbumName())
                                .collect(Collectors.toList())
                )
                .numberOfFollowers(totalNumberOfUserFollowers(artist.getId()))
                .build();
    }

    @Override
    public ArtistPresentation viewArtistProfile(Long id) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        String formattedDateOfBirth = artist.getBirthDay() != null ?
                artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedCreatedDate = artist.getCreatedDate() != null
                ? formatter.format(artist.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;
        String formattedLastModifiedDate = artist.getLastModifiedDate() != null
                ? formatter.format(artist.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                : null;

        return ArtistPresentation.builder()
                .id(artist.getId())
                .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                .artistName(artist.getArtistName() != null ? artist.getArtistName() : "")
                .email(artist.getEmail())
                .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                .birthDay(formattedDateOfBirth)
                .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                .status(artist.getStatus())
                .createdBy(artist.getCreatedBy())
                .lastModifiedBy(artist.getLastModifiedBy())
                .createdDate(formattedCreatedDate)
                .lastModifiedDate(formattedLastModifiedDate)
                .description(artist.getDescription() != null ? artist.getDescription() : "")
                .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                .userType(artist.getUserType())
                .artistSongIds(
                        artist.getArtistSongs().stream()
                                .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistSongId().getSong().getId())
                                .collect(Collectors.toList())
                )
                .artistSongNameList(
                        artist.getArtistSongs().stream()
                                .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistSongId().getSong().getTitle())
                                .collect(Collectors.toList())
                )
                .artistPlaylistIds(
                        artist.getArtistPlaylists().stream()
                                .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getId())
                                .collect(Collectors.toList())
                )
                .artistPlaylistNameList(
                        artist.getArtistPlaylists().stream()
                                .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getPlaylistName())
                                .collect(Collectors.toList())
                )
                .artistAlbumIds(
                        artist.getArtistAlbums().stream()
                                .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getId())
                                .collect(Collectors.toList())
                )
                .artistAlbumNameList(
                        artist.getArtistAlbums().stream()
                                .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getAlbumName())
                                .collect(Collectors.toList())
                )
                .numberOfFollowers(totalNumberOfUserFollowers(artist.getId()))
                .build();
    }

    @Override
    public List<AdminPresentation> getAllAdmin() {
        List<User> admins = userRepository.findByUserType(UserType.ADMIN);

        return admins.stream().map(admin -> {
            String formattedDate = admin.getBirthDay() != null
                    ? admin.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String formattedCreatedDate = admin.getCreatedDate() != null
                    ? formatter.format(admin.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;
            String formattedLastModifiedDate = admin.getLastModifiedDate() != null
                    ? formatter.format(admin.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;

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
                    .createdDate(formattedCreatedDate)
                    .lastModifiedDate(formattedLastModifiedDate)
                    .build();
        }).toList();
    }

    @Override
    public List<UserPresentation> getAllUser() {
        List<User> users = userRepository.findByUserType(UserType.USER);

        return users.stream().map(user -> {
            String formattedDate = user.getBirthDay() != null
                    ? user.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String formattedCreatedDate = user.getCreatedDate() != null
                    ? formatter.format(user.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;
            String formattedLastModifiedDate = user.getLastModifiedDate() != null
                    ? formatter.format(user.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;

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
                    .createdDate(formattedCreatedDate)
                    .lastModifiedDate(formattedLastModifiedDate)
                    .build();
        }).toList();
    }

    @Override
    public List<ArtistPresentation> getAllArtist() {
        List<Artist> artists = artistRepository.findAll();

        return artists.stream().map(artist -> {
            String formattedDate = artist.getBirthDay() != null
                    ? artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String formattedCreatedDate = artist.getCreatedDate() != null
                    ? formatter.format(artist.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;
            String formattedLastModifiedDate = artist.getLastModifiedDate() != null
                    ? formatter.format(artist.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;

            return ArtistPresentation.builder()
                    .id(artist.getId())
                    .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                    .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                    .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                    .artistName(artist.getArtistName() != null ? artist.getArtistName() : "")
                    .email(artist.getEmail())
                    .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                    .birthDay(formattedDate)
                    .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                    .status(artist.getStatus())
                    .createdBy(artist.getCreatedBy())
                    .lastModifiedBy(artist.getLastModifiedBy())
                    .createdDate(formattedCreatedDate)
                    .lastModifiedDate(formattedLastModifiedDate)
                    .description(artist.getDescription() != null ? artist.getDescription() : "")
                    .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                    .userType(artist.getUserType())
                    .artistSongIds(
                            artist.getArtistSongs().stream()
                                    .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistSongId().getSong().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistSongNameList(
                            artist.getArtistSongs().stream()
                                    .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistSongId().getSong().getTitle())
                                    .collect(Collectors.toList())
                    )
                    .artistPlaylistIds(
                            artist.getArtistPlaylists().stream()
                                    .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistPlaylistNameList(
                            artist.getArtistPlaylists().stream()
                                    .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getPlaylistName())
                                    .collect(Collectors.toList())
                    )
                    .artistAlbumIds(
                            artist.getArtistAlbums().stream()
                                    .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistAlbumNameList(
                            artist.getArtistAlbums().stream()
                                    .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getAlbumName())
                                    .collect(Collectors.toList())
                    )
                    .numberOfFollowers(totalNumberOfUserFollowers(artist.getId()))
                    .build();
        }).toList();
    }

    @Override
    public List<ArtistPresentation> getAllOtherArtist() {
        Long id = jwtHelper.getIdUserRequesting();
        List<Artist> artists = artistRepository.findAll().stream()
                .filter(a -> !a.getId().equals(id))
                .filter(a -> !a.getStatus().equals(CommonStatus.INACTIVE.getStatus()))
                .toList();

        return artists.stream().map(artist -> {
            String formattedDate = artist.getBirthDay() != null
                    ? artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String formattedCreatedDate = artist.getCreatedDate() != null
                    ? formatter.format(artist.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;
            String formattedLastModifiedDate = artist.getLastModifiedDate() != null
                    ? formatter.format(artist.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;

            return ArtistPresentation.builder()
                    .id(artist.getId())
                    .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                    .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                    .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                    .artistName(artist.getArtistName() != null ? artist.getArtistName() : "")
                    .email(artist.getEmail())
                    .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                    .birthDay(formattedDate)
                    .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                    .status(artist.getStatus())
                    .createdBy(artist.getCreatedBy())
                    .lastModifiedBy(artist.getLastModifiedBy())
                    .createdDate(formattedCreatedDate)
                    .lastModifiedDate(formattedLastModifiedDate)
                    .description(artist.getDescription() != null ? artist.getDescription() : "")
                    .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                    .userType(artist.getUserType())
                    .artistSongIds(
                            artist.getArtistSongs().stream()
                                    .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistSongId().getSong().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistSongNameList(
                            artist.getArtistSongs().stream()
                                    .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistSongId().getSong().getTitle())
                                    .collect(Collectors.toList())
                    )
                    .artistPlaylistIds(
                            artist.getArtistPlaylists().stream()
                                    .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistPlaylistNameList(
                            artist.getArtistPlaylists().stream()
                                    .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getPlaylistName())
                                    .collect(Collectors.toList())
                    )
                    .artistAlbumIds(
                            artist.getArtistAlbums().stream()
                                    .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistAlbumNameList(
                            artist.getArtistAlbums().stream()
                                    .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getAlbumName())
                                    .collect(Collectors.toList())
                    )
                    .numberOfFollowers(totalNumberOfUserFollowers(artist.getId()))
                    .build();
        }).toList();
    }

    @Override
    public List<ArtistPresentation> getAllActiveArtists() {
        Long id = jwtHelper.getIdUserRequesting();
        List<Artist> artists = artistRepository.findAll().stream()
                .filter(a -> !(a.getId().equals(id) && a.getStatus().equals(CommonStatus.INACTIVE.getStatus())))
                .toList();

        return artists.stream().map(artist -> {
            String formattedDate = artist.getBirthDay() != null
                    ? artist.getBirthDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String formattedCreatedDate = artist.getCreatedDate() != null
                    ? formatter.format(artist.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;
            String formattedLastModifiedDate = artist.getLastModifiedDate() != null
                    ? formatter.format(artist.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;

            return ArtistPresentation.builder()
                    .id(artist.getId())
                    .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                    .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                    .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                    .artistName(artist.getArtistName() != null ? artist.getArtistName() : "")
                    .email(artist.getEmail())
                    .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                    .birthDay(formattedDate)
                    .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                    .status(artist.getStatus())
                    .createdBy(artist.getCreatedBy())
                    .lastModifiedBy(artist.getLastModifiedBy())
                    .createdDate(formattedCreatedDate)
                    .lastModifiedDate(formattedLastModifiedDate)
                    .description(artist.getDescription() != null ? artist.getDescription() : "")
                    .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                    .userType(artist.getUserType())
                    .artistSongIds(
                            artist.getArtistSongs().stream()
                                    .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistSongId().getSong().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistSongNameList(
                            artist.getArtistSongs().stream()
                                    .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistSongId().getSong().getTitle())
                                    .collect(Collectors.toList())
                    )
                    .artistPlaylistIds(
                            artist.getArtistPlaylists().stream()
                                    .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistPlaylistNameList(
                            artist.getArtistPlaylists().stream()
                                    .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getPlaylistName())
                                    .collect(Collectors.toList())
                    )
                    .artistAlbumIds(
                            artist.getArtistAlbums().stream()
                                    .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistAlbumNameList(
                            artist.getArtistAlbums().stream()
                                    .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getAlbumName())
                                    .collect(Collectors.toList())
                    )
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
    public ApiResponse signUpUser(CreateUser request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        User user = User.builder()
                .email(request.getEmail())
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

        return ApiResponse.ok();
    }

    // Forgot Password Request
    @Override
    public Map<String, ZonedDateTime> forgotPasswordBegin(SendOtpRequest request) {
        return sendOtpToEmail(request);
    }

    @Override
    public Map<String, Boolean> forgotPasswordCheckOtp(CheckOtpRequest request) {
        return checkOtp(request);
    }

    @Override
    public ApiResponse forgotPasswordFinish(ForgotPasswordFinish request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        String sessionId = request.getSessionId();
        if (!sessionOtpMap.containsKey(sessionId)) {
            throw new BusinessException(ApiResponseCode.SESSION_ID_NOT_FOUND);
        }
        userRepository
                .save(((User) userService
                        .loadUserByUsername(sessionOtpMap.get(sessionId).getEmail()))
                        .toBuilder()
                        .password(passwordEncoder.encode(request.getPassword()))
                        .lastModifiedDate(now)
                        .build());
        sessionOtpMap.remove(sessionId);
        return ApiResponse.ok();
    }

    // Reset Password Request
    @Override
    public ApiResponse resetPassword(ChangePasswordRequest request) {
        User user = userRepository.findById(jwtHelper.getIdUserRequesting())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the old password");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.ok();
    }

    // Update Account Request
    @Override
    public ApiResponse updateAccount(UpdateAccountRequest request) {
        Long id = jwtHelper.getIdUserRequesting();
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

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
        user.setLastModifiedDate(now);
        userRepository.save(user);

        return ApiResponse.ok();
    }

    @Override
    public ApiResponse updateArtistAccount(UpdateAccountRequest request) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Long id = jwtHelper.getIdUserRequesting();
        Artist user = artistRepository.findById(id)
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

        if (request.getBackgroundImage() != null && !request.getBackgroundImage().isEmpty()) {
            String backgroundImageUrl = cloudinaryService.uploadImageToCloudinary(request.getBackgroundImage());
            user.setImageUrl(backgroundImageUrl);
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            user.setDescription(request.getDescription());
        }

        user.setLastModifiedBy(id);
        user.setLastModifiedDate(now);
        artistRepository.save(user);

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
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        user.setStatus(CommonStatus.DELETED.getStatus());
        user.setLastModifiedBy(user.getId());
        user.setLastModifiedDate(now);
        userRepository.save(user); // Chỉ cập nhật trạng thái
    }

    private void handleArtistDeletion(User user) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        user.setStatus(CommonStatus.INACTIVE.getStatus());
        user.setLastModifiedBy(user.getId());
        user.setLastModifiedDate(now);
        userRepository.save(user);
    }

    private void handleAdminDeactivation(User user) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        user.setStatus(CommonStatus.LOCKED.getStatus());
        user.setLastModifiedBy(user.getId());
        user.setLastModifiedDate(now);
        userRepository.save(user);
    }

    @Scheduled(fixedRate = 5000)
    public void deleteUsersMarkedAsDeleted() {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Instant tenMinutesAgo = now.minus(Duration.ofMinutes(10));
        List<User> usersToDelete = userRepository.findAllByStatusAndLastModifiedDateBefore(
                CommonStatus.DELETED.getStatus(), tenMinutesAgo
        );

        userRepository.deleteAll(usersToDelete);
    }

    @Override
    public ApiResponse adminDeleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        userRepository.delete(user);

        return ApiResponse.ok();
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

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    String formattedCreatedDate = admin.getCreatedDate() != null
                            ? formatter.format(admin.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                            : null;
                    String formattedLastModifiedDate = admin.getLastModifiedDate() != null
                            ? formatter.format(admin.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                            : null;

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
                            .createdDate(formattedCreatedDate)
                            .lastModifiedDate(formattedLastModifiedDate)
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

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String formattedCreatedDate = artist.getCreatedDate() != null
                    ? formatter.format(artist.getCreatedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;
            String formattedLastModifiedDate = artist.getLastModifiedDate() != null
                    ? formatter.format(artist.getLastModifiedDate().atZone(ZoneId.of("Asia/Ho_Chi_Minh")))
                    : null;

            return ArtistPresentation.builder()
                    .id(artist.getId())
                    .avatar(artist.getAvatar() != null ? artist.getAvatar() : "")
                    .firstName(artist.getFirstName() != null ? artist.getFirstName() : "")
                    .lastName(artist.getLastName() != null ? artist.getLastName() : "")
                    .artistName(artist.getArtistName() != null ? artist.getArtistName() : "")
                    .email(artist.getEmail())
                    .gender(artist.getGender() != null ? artist.getGender().toString() : "")
                    .birthDay(formattedDate)
                    .phone(artist.getPhoneNumber() != null ? artist.getPhoneNumber() : "")
                    .status(artist.getStatus())
                    .createdBy(artist.getCreatedBy())
                    .lastModifiedBy(artist.getLastModifiedBy())
                    .createdDate(formattedCreatedDate)
                    .lastModifiedDate(formattedLastModifiedDate)
                    .description(artist.getDescription() != null ? artist.getDescription() : "")
                    .image(artist.getImageUrl() != null ? artist.getImageUrl() : "")
                    .userType(artist.getUserType())
                    .artistSongIds(
                            artist.getArtistSongs().stream()
                                    .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistSongId().getSong().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistSongNameList(
                            artist.getArtistSongs().stream()
                                    .filter(artistSong -> artistSong.getArtistSongId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistSongId().getSong().getTitle())
                                    .collect(Collectors.toList())
                    )
                    .artistPlaylistIds(
                            artist.getArtistPlaylists().stream()
                                    .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistPlaylistNameList(
                            artist.getArtistPlaylists().stream()
                                    .filter(artistSong -> artistSong.getArtistPlaylistId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistPlaylistId().getPlaylist().getPlaylistName())
                                    .collect(Collectors.toList())
                    )
                    .artistAlbumIds(
                            artist.getArtistAlbums().stream()
                                    .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getId())
                                    .collect(Collectors.toList())
                    )
                    .artistAlbumNameList(
                            artist.getArtistAlbums().stream()
                                    .filter(artistSong -> artistSong.getArtistAlbumId().getArtist().getId().equals(artist.getId()))
                                    .map(artistSong -> artistSong.getArtistAlbumId().getAlbum().getAlbumName())
                                    .collect(Collectors.toList())
                    )
                    .numberOfFollowers(totalNumberOfUserFollowers(artist.getId()))
                    .build();
        }).toList();
    }

    @Override
    public ApiResponse processingDeleteRequest(Long userId, String manageFunction) {
        ZonedDateTime dueDateInVietnam = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(60);
        Instant now = dueDateInVietnam.toInstant();

        Long userModifyId = jwtHelper.getIdUserRequesting();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        if (user.getUserType() == UserType.USER) {
            throw new BusinessException(ApiResponseCode.INVALID_TYPE);
        }

        int currentStatus = user.getStatus();
        boolean isArtist = user.getUserType() == UserType.ARTIST;
        boolean validToProcess = (isArtist && currentStatus == CommonStatus.INACTIVE.getStatus()) ||
                                 (!isArtist && currentStatus == CommonStatus.LOCKED.getStatus());

        if (!validToProcess) {
            throw new BusinessException(ApiResponseCode.INVALID_STATUS);
        }

        ManageProcess process;
        try {
            process = ManageProcess.valueOf(manageFunction.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ApiResponseCode.INVALID_TYPE);
        }

        if (process == ManageProcess.ACCEPTED) {
            // Nếu ACCEPTED thì xoá (set status = DELETED)
            user.setStatus(CommonStatus.DELETED.getStatus());
        } else if (process == ManageProcess.REVOKED) {
            // Nếu REVOKED thì kích hoạt lại (set status = ACTIVE)
            user.setStatus(CommonStatus.ACTIVE.getStatus());
        } else {
            throw new BusinessException(ApiResponseCode.INVALID_TYPE);
        }

        user.setLastModifiedBy(userModifyId);
        user.setLastModifiedDate(now);
        userRepository.save(user);

        String message = (process == ManageProcess.ACCEPTED)
                ? "Account deleted successfully!"
                : "Account revoked successfully!";

        return ApiResponse.ok(message);
    }

    // Helpers
    public Long totalNumberOfUserFollowers(Long artistId) {
        return artistUserFollowRepository.countDistinctUsersByArtistId(artistId);
    }
}
