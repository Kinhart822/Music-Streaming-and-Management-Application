package com.spring.controller.user;

import com.spring.dto.response.ArtistPresentation;
import com.spring.dto.response.UserPresentation;
import com.spring.dto.response.ApiResponse;
import com.spring.service.AccountService;
import com.spring.service.UserSongCountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final AccountService accountService;
    private final UserSongCountService userSongCountService;

    @GetMapping("/viewAnotherUserProfile/{id}")
    public ResponseEntity<UserPresentation> viewAnotherUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.viewUserProfile(id));
    }

    @GetMapping("/viewArtistProfile/{id}")
    public ResponseEntity<ArtistPresentation> viewArtistProfile(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.viewArtistProfile(id));
    }

    @PostMapping("/listen/{songId}")
    public ResponseEntity<ApiResponse> recordSongListen(@PathVariable Long songId) {
        return ResponseEntity.ok(userSongCountService.incrementListenCount(songId));
    }
}
