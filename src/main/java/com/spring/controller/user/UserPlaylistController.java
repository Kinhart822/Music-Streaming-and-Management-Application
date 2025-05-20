package com.spring.controller.user;

import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.PlaylistResponse;
import com.spring.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/playlist")
@RequiredArgsConstructor
public class UserPlaylistController {
    private final PlaylistService playlistService;

    @PostMapping("/userSavePlaylist/{id}")
    public ResponseEntity<ApiResponse> userSavePlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.userSavePlaylist(id));
    }

    @DeleteMapping("/userUnSavePlaylist/{id}")
    public ResponseEntity<ApiResponse> userUnSavePlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.userUnSavePlaylist(id));
    }

    @GetMapping("/info/{id}")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getPlaylistById(id));
    }

    @GetMapping("/infoAll/{id}")
    public ResponseEntity<List<PlaylistResponse>> getAllPlaylistsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getAllPlaylistsByArtistId(id));
    }

    @GetMapping("/saved-playlists")
    public ResponseEntity<List<PlaylistResponse>> getAllUserSavedPlaylists() {
        return ResponseEntity.ok(playlistService.getCurrentUserSavedPlaylists());
    }

    @GetMapping("/recent-playlists")
    public ResponseEntity<List<PlaylistResponse>> getRecentPlaylists() {
        return ResponseEntity.ok(playlistService.getRecentCurrentUserSavedPlaylists());
    }

    @GetMapping("/saved-playlist-check/{id}")
    public ResponseEntity<Boolean> checkFollowed(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.isSavedPlaylist(id));
    }
}
