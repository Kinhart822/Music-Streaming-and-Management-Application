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
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final RefreshTokenRepository refreshTokenRepository;
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
    public ApiResponse createUser(CreateUser request) {
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

        return AdminPresentation.builder()
                .id(admin.getId())
                .avatar(admin.getAvatar())
                .username(admin.getUsername())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .email(admin.getEmail())
                .gender(admin.getGender().toString())
                .birthDay(admin.getBirthDay())
                .phone(admin.getPhoneNumber())
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

        return UserPresentation.builder()
                .id(user.getId())
                .avatar(user.getAvatar())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .gender(user.getGender().toString())
                .birthDay(user.getBirthDay())
                .phone(user.getPhoneNumber())
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
                .orElseThrow(()-> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        return ArtistPresentation.builder()
                .id(artist.getId())
                .avatar(artist.getAvatar())
                .username(artist.getUsername())
                .firstName(artist.getFirstName())
                .lastName(artist.getLastName())
                .description(artist_info.getDescription())
                .image(artist_info.getImage())
                .countListen(artist_info.getCountListen())
                .email(artist.getEmail())
                .gender(artist.getGender().toString())
                .birthDay(artist.getBirthDay())
                .phone(artist.getPhoneNumber())
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
        userRepository
                .save(User.builder()
                        .email(sessionOtpMap.get(sessionId).getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .userType(UserType.USER)
                        .status(CommonStatus.ACTIVE.getStatus())
                        .createdDate(now)
                        .lastModifiedDate(now)
                        .build());
        sessionOtpMap.remove(request.getSessionId());
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
        userRepository
                .save(userRepository
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND))
                        .toBuilder()
                        .avatar(request.getAvatar())
                        .username(request.getUsername())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .gender(request.getGender())
                        .birthDay(request.getDateOfBirth())
                        .phoneNumber(request.getPhone())
                        .lastModifiedBy(id)
                        .lastModifiedDate(Instant.now())
                        .build());
        return ApiResponse.ok();
    }

    @Override
    public void deleteAccount(Long userId) {
        // Tìm người dùng theo ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.ENTITY_NOT_FOUND));

        // Xóa tất cả thông tin liên quan đến người dùng
        refreshTokenRepository.deleteAllByUser(user);
        notificationRepository.deleteAllByUser(user);

        // Xóa người dùng khỏi cơ sở dữ liệu
        userRepository.delete(user);
    }
}
