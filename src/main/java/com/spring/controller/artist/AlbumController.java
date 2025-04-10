package com.spring.controller.artist;

import com.spring.dto.request.music.artist.AddSongRequest;
import com.spring.dto.request.music.artist.AlbumRequest;
import com.spring.dto.request.music.artist.RemoveSongRequest;
import com.spring.dto.response.AlbumResponse;
import com.spring.dto.response.ApiResponse;
import com.spring.service.AlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/artist/album")
@RequiredArgsConstructor
public class AlbumController {
    private final AlbumService albumService;

    @PostMapping("/create")
    public ResponseEntity<AlbumResponse> createAlbum(@ModelAttribute @Valid AlbumRequest request) {
        return ResponseEntity.ok(albumService.createAlbum(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<AlbumResponse> updatePlaylist(@PathVariable Long id, @ModelAttribute @Valid AlbumRequest request) {
        return ResponseEntity.ok(albumService.updateAlbum(id, request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.deleteAlbum(id));
    }

    @GetMapping("/info/{id}")
    public ResponseEntity<AlbumResponse> getAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAlbumById(id));
    }

    @GetMapping("/infoAll/currentArtist")
    public ResponseEntity<List<AlbumResponse>> getAllPlaylistsByCurrentAccount() {
        return ResponseEntity.ok(albumService.getAllAlbumsByCurrentAccount());
    }

    @GetMapping("/infoAll/{id}")
    public ResponseEntity<List<AlbumResponse>> getAllPlaylistsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAllAlbumsByArtistId(id));
    }

    @PostMapping("/song/add")
    public ResponseEntity<ApiResponse> addSongToAlbum(@RequestBody AddSongRequest request) {
        return ResponseEntity.ok(albumService.addSongToAlbum(request));
    }

    @PostMapping("/songList/add")
    public ResponseEntity<ApiResponse> addListSongToAlbum(@RequestBody AddSongRequest request) {
        return ResponseEntity.ok(albumService.addListSongToAlbum(request));
    }

    @DeleteMapping("/song/remove")
    public ResponseEntity<ApiResponse> removeSongFromAlbum(@RequestBody RemoveSongRequest request) {
        return ResponseEntity.ok(albumService.removeSongFromAlbum(request));
    }

    @DeleteMapping("/songList/remove")
    public ResponseEntity<ApiResponse> removeListSongFromAlbum(@RequestBody RemoveSongRequest request) {
        return ResponseEntity.ok(albumService.removeListSongFromAlbum(request));
    }

    @PostMapping("/upload/{id}")
    public ResponseEntity<ApiResponse> uploadAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.uploadAlbum(id));
    }
}
