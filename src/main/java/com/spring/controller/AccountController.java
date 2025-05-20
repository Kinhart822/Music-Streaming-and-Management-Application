package com.spring.controller;

import com.spring.constants.ApiResponseCode;
import com.spring.constants.Gender;
import com.spring.dto.request.account.*;
import com.spring.dto.response.*;
import com.spring.entities.Notification;
import com.spring.exceptions.BusinessException;
import com.spring.repository.PlaylistRepository;
import com.spring.security.JwtHelper;
import com.spring.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final JwtHelper jwtHelper;
    private final SongService songService;
    private final GenreService genreService;
    private final UserSongCountService userSongCountService;
    private final LikeFollowingDownloadService likeFollowingDownloadService;

    /*
        TODO: Reset Password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPasswordFinish(@RequestBody @Valid ChangePasswordRequest request) {
        return ResponseEntity.ok(accountService.resetPassword(request));
    }

    /*
        TODO: Update account details
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse> updateAccount(@ModelAttribute @Valid UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(request));
    }

    @PutMapping(value = "/updateArtist", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> updateArtistAccount(
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "backgroundImage", required = false) MultipartFile backgroundImage,
            @RequestPart(value = "firstName", required = false) String firstName,
            @RequestPart(value = "lastName", required = false) String lastName,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "gender", required = false) String gender,
            @RequestPart(value = "dateOfBirth", required = false) String dateOfBirth,
            @RequestPart(value = "phone", required = false) String phone) {
        log.info("Received updateArtist request - Avatar: {}, BackgroundImage: {}, FirstName: {}, LastName: {}, Description: {}, Gender: {}, DateOfBirth: {}, Phone: {}",
                avatar != null ? avatar.getOriginalFilename() : null,
                backgroundImage != null ? backgroundImage.getOriginalFilename() : null,
                firstName, lastName, description, gender, dateOfBirth, phone);

        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setAvatar(avatar);
        request.setBackgroundImage(backgroundImage);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setDescription(description);
        if (gender != null) {
            try {
                request.setGender(Gender.valueOf(gender));
            } catch (IllegalArgumentException e) {
                log.error("Invalid gender value: {}", gender);
                throw new BusinessException(ApiResponseCode.INVALID_HTTP_REQUEST);
            }
        }
        request.setDateOfBirth(dateOfBirth);
        request.setPhone(phone);

        return ResponseEntity.ok(accountService.updateArtistAccount(request));
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
    @PostMapping("/signUpArtist")
    public ResponseEntity<ApiResponse> signUpArtist(@RequestBody @Valid CreateArtist request) {
        return ResponseEntity.ok(accountService.signUpArtist(request));
    }

    @PostMapping("/admin/create")
    public ResponseEntity<ApiResponse> createAdmin(@RequestBody @Valid CreateAdmin request) {
        return ResponseEntity.ok(accountService.createAdmin(request));
    }

    @GetMapping("/user/sign-up/check-email-existence")
    public ResponseEntity<Map<String, Boolean>> signUpCheckEmailExistence(@RequestParam String query) {
        return ResponseEntity.ok(accountService.signUpCheckEmailExistence(query));
    }

    @PostMapping("/signUpUser")
    public ResponseEntity<ApiResponse> signUpUser(@RequestBody @Valid CreateUser request) {
        return ResponseEntity.ok(accountService.signUpUser(request));
    }

    @PostMapping("/user/forgot-password/begin")
    public ResponseEntity<Map<String, ZonedDateTime>> forgotPasswordBegin(@RequestBody SendOtpRequest request) {
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
        TODO: Song, Genre
    */
    @GetMapping("/song/infoAll")
    public ResponseEntity<List<SongResponse>> getAllSongs() {
        return ResponseEntity.ok(songService.getAllSongs());
    }

    @GetMapping("/song/info/{id}")
    public ResponseEntity<SongResponse> getSongById(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getSongById(id));
    }

    @GetMapping("/getAllGenre")
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        List<GenreResponse> genres = genreService.getAllGenres();
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/song/genre/{genreId}")
    public ResponseEntity<List<SongResponse>> getSongsByGenre(@PathVariable Long genreId) {
        return ResponseEntity.ok(songService.getSongsByGenre(genreId));
    }

    @GetMapping("/song/listeners/{songId}")
    public ResponseEntity<Long> getNumberOfListener(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfListener(songId));
    }

    @GetMapping("/song/downloads/{songId}")
    public ResponseEntity<Long> getNumberOfDownload(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfDownload(songId));
    }

    @GetMapping("/song/likes/{songId}")
    public ResponseEntity<Long> getNumberOfUserLike(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfUserLike(songId));
    }

    @GetMapping("/song/count-listen/{songId}")
    public ResponseEntity<Long> getCountListen(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getCountListen(songId));
    }

    @GetMapping("/song/top10/trending")
    public ResponseEntity<List<SongResponse>> getTop10TrendingSongs() {
        return ResponseEntity.ok(songService.getTop10TrendingSongs());
    }

    @GetMapping("/song/top15/download")
    public ResponseEntity<List<SongResponse>> getTop15BestSong() {
        return ResponseEntity.ok(songService.getTop15MostDownloadSong());
    }

    @GetMapping("/viewHistoryListen")
    public ResponseEntity<List<HistoryListenResponse>> viewHistoryListen() {
        return ResponseEntity.ok(userSongCountService.getAllHistoryListenByCurrentUser());
    }

    @GetMapping("/recentListening")
    public ResponseEntity<List<HistoryListenResponse>> recentListening() {
        return ResponseEntity.ok(userSongCountService.getAllRecentListeningCurrentUser());
    }

    @GetMapping("/recentViewArtist")
    public ResponseEntity<List<ArtistPresentation>> getRecentFollowedArtistsOfUser() {
        return ResponseEntity.ok(likeFollowingDownloadService.getRecentFollowedArtistsOfUser());
    }
}
