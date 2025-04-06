package com.spring.service;

import com.spring.dto.request.account.*;
import com.spring.dto.response.ApiResponse;
import com.spring.entities.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface AccountService {
    ApiResponse createAdmin(CreateAdmin request);

    ApiResponse createArtist(CreateArtist request);

    ApiResponse createArtistFromList(CreateArtistFromList request);

    Map<String, Instant> sendOtpToEmail(SendOtpRequest request);

    Map<String, Boolean> checkOtp(CheckOtpRequest request);

    List<Notification> findAllNotifications();

    // Account Information
    AdminPresentation getAdmin();

    UserPresentation getUser();

    ArtistPresentation getArtist();

    // Sign Up Steps
    Map<String, Boolean> signUpCheckEmailExistence(String query);

    Map<String, Instant> signUpBegin(SendOtpRequest request);

    Map<String, Boolean> signUpCheckOtp(CheckOtpRequest request);

    ApiResponse signUpFinish(SignUpRequest request);

    // Forgot Password Steps
    Map<String, Instant> forgotPasswordBegin(SendOtpRequest request);

    Map<String, Boolean> forgotPasswordCheckOtp(CheckOtpRequest request);

    ApiResponse forgotPasswordFinish(ForgotPasswordFinish request);

    // Reset Password Steps
    ApiResponse resetPasswordRequest(ResetPasswordRequest request);

    Map<String, Boolean> resetPasswordCheck(ResetPasswordCheck check);

    ApiResponse resetPasswordFinish(ResetPasswordFinish finish);

    // Update Account Step
    ApiResponse updateAccount(UpdateAccountRequest request);

    // Delete Account Step
    ApiResponse deleteAccount(Long userId);
}
