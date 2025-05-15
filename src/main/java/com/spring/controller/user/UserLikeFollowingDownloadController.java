package com.spring.controller.user;

import com.spring.dto.response.ArtistPresentation;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.SongResponse;
import com.spring.service.LikeFollowingDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserLikeFollowingDownloadController {
    private final LikeFollowingDownloadService likeFollowingDownloadService;

    // TODO: Follow
    @PostMapping("/follow/artist/{artistId}")
    public ResponseEntity<ApiResponse> followArtist(@PathVariable Long artistId) {
        return ResponseEntity.ok(likeFollowingDownloadService.userFollowingArtist(artistId));
    }

    @DeleteMapping("/unfollow/artist/{artistId}")
    public ResponseEntity<ApiResponse> unfollowArtist(@PathVariable Long artistId) {
        return ResponseEntity.ok(likeFollowingDownloadService.userUnfollowingArtist(artistId));
    }

    @GetMapping("/followed-artists")
    public ResponseEntity<List<ArtistPresentation>> getFollowedArtists() {
        return ResponseEntity.ok(likeFollowingDownloadService.getCurrentUserFollowedArtists());
    }

    // TODO: Favourite Songs
    @PostMapping("/likeSong/{songId}")
    public ResponseEntity<ApiResponse> userLikeSong(@PathVariable Long songId) {
        return ResponseEntity.ok(likeFollowingDownloadService.userLikeSong(songId));
    }

    @DeleteMapping("/unlikeSong/{songId}")
    public ResponseEntity<ApiResponse> userUnlikeSong(@PathVariable Long songId) {
        return ResponseEntity.ok(likeFollowingDownloadService.userUnlikeSong(songId));
    }

    @GetMapping("/liked-check/{songId}")
    public ResponseEntity<Boolean> checkLiked(@PathVariable Long songId) {
        return ResponseEntity.ok(likeFollowingDownloadService.isFavoriteSong(songId));
    }

    @GetMapping("/liked-songs")
    public ResponseEntity<List<SongResponse>> getLikedSongs() {
        return ResponseEntity.ok(likeFollowingDownloadService.getCurrentUserLikedSongs());
    }

    // TODO: Download
    @PostMapping("/downloadSong/{songId}")
    public ResponseEntity<ApiResponse> userDownloadSong(@PathVariable Long songId) {
        return ResponseEntity.ok(likeFollowingDownloadService.userDownloadSong(songId));
    }

    @DeleteMapping("/undownloadSong/{songId}")
    public ResponseEntity<ApiResponse> userUndownloadSong(@PathVariable Long songId) {
        return ResponseEntity.ok(likeFollowingDownloadService.userUndownloadSong(songId));
    }

    @GetMapping("/downloaded-songs")
    public ResponseEntity<List<String>> getDownloadedSongs() {
        List<String> songTitles = likeFollowingDownloadService.getUserDownloadedSongs();
        return ResponseEntity.ok(songTitles);
    }
}
