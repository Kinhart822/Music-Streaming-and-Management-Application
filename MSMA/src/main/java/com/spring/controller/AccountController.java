package com.spring.controller;

import com.spring.dto.request.account.*;
import com.spring.dto.response.ApiResponse;
import com.spring.entities.Notification;
import com.spring.security.JwtHelper;
import com.spring.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final JwtHelper jwtHelper;

    /*
        TODO: Reset Password
     */
    @PostMapping("/reset-password/request")
    public ResponseEntity<ApiResponse> resetPasswordRequest(@RequestBody @Valid ResetPasswordRequest request) {
        return ResponseEntity.ok(accountService.resetPasswordRequest(request));
    }

    @GetMapping("/reset-password/check")
    public ResponseEntity<Map<String, Boolean>> resetPasswordCheck(@RequestBody ResetPasswordCheck check) {
        return ResponseEntity.ok(accountService.resetPasswordCheck(check));
    }

    @PostMapping("/reset-password/finish")
    public ResponseEntity<ApiResponse> resetPasswordFinish(@RequestBody @Valid ResetPasswordFinish finish) {
        return ResponseEntity.ok(accountService.resetPasswordFinish(finish));
    }

    /*
        TODO: Update account details
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse> updateAccount(@RequestBody @Valid UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(request));
    }

    /*
    TODO: Delete account details
    */
    @PutMapping("/delete")
    public ResponseEntity<ApiResponse> deleteAccount(HttpServletRequest request) {
        Long userId = jwtHelper.getIdUserRequesting();
        return ResponseEntity.ok(accountService.deleteAccount(userId));
    }

    /*
        TODO: ADMIN specific
     */
    @PostMapping("/admin/create")
    public ResponseEntity<ApiResponse> createAdmin(@RequestBody @Valid CreateAdmin request) {
        return ResponseEntity.ok(accountService.createAdmin(request));
    }

    @PostMapping("/admin/create/batch")
    public ResponseEntity<ApiResponse> createAdminFromList(@RequestBody @Valid CreateAdminFromList request) {
        return ResponseEntity.ok(accountService.createAdminFromList(request));
    }

    @GetMapping("/admin/profile")
    public ResponseEntity<AdminPresentation> getAdmin() {
        return ResponseEntity.ok(accountService.getAdmin());
    }

    /*
    TODO: ARTIST specific
 */
    @PostMapping("/artist/create")
    public ResponseEntity<ApiResponse> createArtist(@RequestBody @Valid CreateArtist request) {
        return ResponseEntity.ok(accountService.createArtist(request));
    }

    @PostMapping("/artist/create/batch")
    public ResponseEntity<ApiResponse> createArtistFromList(@RequestBody @Valid CreateArtistFromList request) {
        return ResponseEntity.ok(accountService.createArtistFromList(request));
    }

    @GetMapping("/artist/profile")
    public ResponseEntity<ArtistPresentation> getArtist(){
        return ResponseEntity.ok(accountService.getArtist());
    }

    /*
        TODO: USER specific
     */
    @GetMapping("/user/sign-up/check-email-existence")
    public ResponseEntity<Map<String, Boolean>> signUpCheckEmailExistence(@RequestParam String query) {
        return ResponseEntity.ok(accountService.signUpCheckEmailExistence(query));
    }

    @PostMapping("/user/sign-up/begin")
    public ResponseEntity<Map<String, Instant>> signUpBegin(@RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(accountService.signUpBegin(request));
    }

    @PostMapping("/user/sign-up/check-otp")
    public ResponseEntity<Map<String, Boolean>> signUpCheckOtp(@RequestBody CheckOtpRequest request) {
        return ResponseEntity.ok(accountService.signUpCheckOtp(request));
    }

    @PostMapping("/user/sign-up/finish")
    public ResponseEntity<ApiResponse> signUpFinish(@RequestBody @Valid SignUpRequest request) {
        return ResponseEntity.ok(accountService.signUpFinish(request));
    }

    @PostMapping("/user/forgot-password/begin")
    public ResponseEntity<Map<String, Instant>> forgotPasswordBegin(@RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(accountService.forgotPasswordBegin(request));
    }

    @PostMapping("/user/forgot-password/check-otp")
    public ResponseEntity<Map<String, Boolean>> forgotPasswordCheckOtp(@RequestBody CheckOtpRequest request) {
        return ResponseEntity.ok(accountService.forgotPasswordCheckOtp(request));
    }

    @PostMapping("/user/forgot-password/finish")
    public ResponseEntity<ApiResponse> forgotPasswordFinish(@RequestBody ForgotPasswordFinish request) {
        return ResponseEntity.ok(accountService.forgotPasswordFinish(request));
    }

    @GetMapping("/user/profile")
    public ResponseEntity<UserPresentation> getUser() {
        return ResponseEntity.ok(accountService.getUser());
    }

    /*
        TODO: Notification
     */
    @GetMapping("/user/notification")
    public ResponseEntity<List<Notification>> findAllNotifications() {
        return ResponseEntity.ok(accountService.findAllNotifications());
    }
}
