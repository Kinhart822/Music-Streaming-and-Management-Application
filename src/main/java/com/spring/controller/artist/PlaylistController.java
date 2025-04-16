package com.spring.controller.artist;

import com.spring.dto.request.music.AddSongRequest;
import com.spring.dto.request.music.PlaylistRequest;
import com.spring.dto.request.music.RemoveSongRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.PlaylistResponse;
import com.spring.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/artist/playlist")
@RequiredArgsConstructor
public class PlaylistController {
    private final PlaylistService playlistService;

    @PostMapping("/create")
    public ResponseEntity<PlaylistResponse> createPlaylist(@ModelAttribute @Valid PlaylistRequest request) {
        return ResponseEntity.ok(playlistService.createPlaylist(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<PlaylistResponse> updatePlaylist(@PathVariable Long id, @ModelAttribute @Valid PlaylistRequest request) {
        return ResponseEntity.ok(playlistService.updatePlaylist(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deletePlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.deletePlaylist(id));
    }

    @GetMapping("/info/{id}")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getPlaylistById(id));
    }

    @GetMapping("/infoAll/currentArtist")
    public ResponseEntity<List<PlaylistResponse>> getAllPlaylistsByCurrentAccount() {
        return ResponseEntity.ok(playlistService.getAllPlaylistsByCurrentAccount());
    }

    @GetMapping("/infoAll/{id}")
    public ResponseEntity<List<PlaylistResponse>> getAllPlaylistsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getAllPlaylistsByArtistId(id));
    }

    @PostMapping("/song/add")
    public ResponseEntity<ApiResponse> addSongToPlaylist(@RequestBody AddSongRequest request) {
        return ResponseEntity.ok(playlistService.addSongToPlaylist(request));
    }

    @PostMapping("/songList/add")
    public ResponseEntity<ApiResponse> addListSongToPlaylist(@RequestBody AddSongRequest request) {
        return ResponseEntity.ok(playlistService.addListSongToPlaylist(request));
    }

    @DeleteMapping("/song/remove")
    public ResponseEntity<ApiResponse> removeSongFromPlaylist(@RequestBody RemoveSongRequest request) {
        return ResponseEntity.ok(playlistService.removeSongFromPlaylist(request));
    }

    @DeleteMapping("/songList/remove")
    public ResponseEntity<ApiResponse> removeListSongFromPlaylist(@RequestBody RemoveSongRequest request) {
        return ResponseEntity.ok(playlistService.removeListSongFromPlaylist(request));
    }

    @PostMapping("/upload/{id}")
    public ResponseEntity<ApiResponse> uploadPlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.uploadPlaylist(id));
    }
}
