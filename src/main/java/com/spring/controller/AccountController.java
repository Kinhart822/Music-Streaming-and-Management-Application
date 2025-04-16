package com.spring.controller;

import com.spring.dto.request.account.*;
import com.spring.dto.response.*;
import com.spring.entities.Notification;
import com.spring.security.JwtHelper;
import com.spring.service.AccountService;
import com.spring.service.SongService;
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
    private final SongService songService;

    /*
        TODO: Reset Password
     */
    @PostMapping("/reset-password/request")
    public ResponseEntity<ApiResponse> resetPasswordRequest(@RequestBody @Valid ResetPasswordRequest request) {
        return ResponseEntity.ok(accountService.resetPasswordRequest(request));
    }

    @PostMapping("/reset-password/check")
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
    public ResponseEntity<ApiResponse> updateAccount(@ModelAttribute @Valid UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(request));
    }

    /*
    TODO: Delete account details
    */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse> deleteAccount() {
        Long userId = jwtHelper.getIdUserRequesting();
        return ResponseEntity.ok(accountService.deleteAccount(userId));
    }

    /*
    TODO: Account functionalities
    */
    @PostMapping("/admin/create")
    public ResponseEntity<ApiResponse> createAdmin(@RequestBody @Valid CreateAdmin request) {
        return ResponseEntity.ok(accountService.createAdmin(request));
    }

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

    /*
        TODO: Notification
     */
    @GetMapping("/notification")
    public ResponseEntity<List<Notification>> findAllNotifications() {
        return ResponseEntity.ok(accountService.findAllNotifications());
    }

    /*
        TODO: Profile
    */
    @GetMapping("/profile/admin")
    public ResponseEntity<AdminPresentation> getAdmin() {
        return ResponseEntity.ok(accountService.getAdmin());
    }

    @GetMapping("/profile/artist")
    public ResponseEntity<ArtistPresentation> getArtist() {
        return ResponseEntity.ok(accountService.getArtist());
    }

    @GetMapping("/profile/user")
    public ResponseEntity<UserPresentation> getUser() {
        return ResponseEntity.ok(accountService.getUser());
    }

    @GetMapping("/profile/infoAll/admin")
    public ResponseEntity<List<AdminPresentation>> getAllAdmins() {
        return ResponseEntity.ok(accountService.getAllAdmin());
    }

    @GetMapping("/profile/infoAll/user")
    public ResponseEntity<List<UserPresentation>> getAllUsers() {
        return ResponseEntity.ok(accountService.getAllUser());
    }

    @GetMapping("/profile/infoAll/artist")
    public ResponseEntity<List<ArtistPresentation>> getAllArtists() {
        return ResponseEntity.ok(accountService.getAllArtist());
    }

    /*
        TODO: Song
    */
    @GetMapping("/song/infoAll")
    public ResponseEntity<List<SongResponse>> getAllSongs() {
        return ResponseEntity.ok(songService.getAllSongs());
    }

    @GetMapping("/song/info/{id}")
    public ResponseEntity<SongResponse> getSongById(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getSongById(id));
    }

    @GetMapping("/song/genre/{genreId}")
    public ResponseEntity<List<SongResponse>> getSongsByGenre(@PathVariable Long genreId) {
        return ResponseEntity.ok(songService.getSongsByGenre(genreId));
    }

    @GetMapping("/song/listeners/{songId}")
    public ResponseEntity<ApiResponse> getNumberOfListener(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfListener(songId));
    }

    @GetMapping("/song/downloads/{songId}")
    public ResponseEntity<ApiResponse> getNumberOfDownload(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfDownload(songId));
    }

    @GetMapping("/song/likes/{songId}")
    public ResponseEntity<ApiResponse> getNumberOfUserLike(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfUserLike(songId));
    }

    @GetMapping("/song/count-listen/{songId}")
    public ResponseEntity<ApiResponse> getCountListen(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getCountListen(songId));
    }

    @GetMapping("/song/trending")
    public ResponseEntity<List<SongResponse>> getTrendingSongs() {
        return ResponseEntity.ok(songService.getTrendingSongs());
    }

    @GetMapping("/song/top15/{genreId}")
    public ResponseEntity<List<SongResponse>> getTop15BestSongEachGenre(@PathVariable Long genreId) {
        return ResponseEntity.ok(songService.getTop15BestSongEachGenre(genreId));
    }

    @GetMapping("/song/status/{status}")
    public ResponseEntity<List<SongResponse>> getSongsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(songService.getSongsByStatus(status));
    }
}
