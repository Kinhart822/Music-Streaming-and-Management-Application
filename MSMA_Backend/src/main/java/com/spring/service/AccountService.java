package com.spring.service;

import com.spring.dto.request.account.*;
import com.spring.dto.response.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public interface AccountService {
    ApiResponse createUser(CreateUser request);

    ApiResponse createAdmin(CreateAdmin request);

    ApiResponse createArtist(CreateArtist request);

    ApiResponse signUpArtist(CreateArtist request);

    Map<String, ZonedDateTime> sendOtpToEmail(SendOtpRequest request);

    Map<String, Boolean> checkOtp(CheckOtpRequest request);

    List<NotificationResponse> findAllNotifications();

    Long countArtists();

    Long countUsers();

    Long countSongs();

    Long countPendingSongs();

    // Account Information
    AdminPresentation getAdmin();

    UserPresentation getUser();

    UserPresentation viewUserProfile(Long id);

    ArtistPresentation getArtist();

    ArtistPresentation viewArtistProfile(Long id);

    List<AdminPresentation> getAllAdmin();

    List<UserPresentation> getAllUser();

    List<ArtistPresentation> getAllArtist();

    List<ArtistPresentation> getAllOtherArtist();

    List<ArtistPresentation> getAllActiveArtists();

    // Sign Up Steps
    Map<String, Boolean> signUpCheckEmailExistence(String query);

    ApiResponse signUpUser(CreateUser request);

    // Forgot Password Steps
    Map<String, ZonedDateTime> forgotPasswordBegin(SendOtpRequest request);

    Map<String, Boolean> forgotPasswordCheckOtp(CheckOtpRequest request);

    ApiResponse forgotPasswordFinish(ForgotPasswordFinish request);

    // Reset Password Steps
    ApiResponse resetPassword(ChangePasswordRequest request);

    // Update Account Step
    ApiResponse updateAccount(UpdateAccountRequest request);

    ApiResponse updateArtistAccount(UpdateAccountRequest request);

    // Delete Account Step
    ApiResponse deleteAccount(Long userId);

    ApiResponse adminDeleteAccount(Long userId);

    // Process Delete Request For Artist and Admin cases
    List<AdminPresentation> getAllAdminByLockedStatus();

    List<ArtistPresentation> getAllArtistByInactiveStatus();

    ApiResponse processingDeleteRequest(Long userId, String manageFunction);
}
