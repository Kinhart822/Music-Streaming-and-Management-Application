package com.spring.controller.user;

import com.spring.dto.response.AlbumResponse;
import com.spring.dto.response.ApiResponse;
import com.spring.service.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/album")
@RequiredArgsConstructor
public class UserAlbumController {
    private final AlbumService albumService;

    @PostMapping("/userSaveAlbum/{id}")
    public ResponseEntity<ApiResponse> userSaveAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.userSaveAlbum(id));
    }

    @DeleteMapping("/userUnSaveAlbum/{id}")
    public ResponseEntity<ApiResponse> userUnSaveAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.userUnSaveAlbum(id));
    }

//    @PostMapping("/create")
//    public ResponseEntity<AlbumResponse> createAlbum(@ModelAttribute @Valid AlbumRequest request) {
//        return ResponseEntity.ok(albumService.createAlbum(request));
//    }
//
//    @PutMapping("/update/{id}")
//    public ResponseEntity<AlbumResponse> updateAlbum(@PathVariable Long id, @ModelAttribute @Valid AlbumRequest request) {
//        return ResponseEntity.ok(albumService.updateAlbum(id, request));
//    }
//
//    @DeleteMapping("/delete/{id}")
//    public ResponseEntity<ApiResponse> deleteAlbum(@PathVariable Long id) {
//        return ResponseEntity.ok(albumService.deleteAlbum(id));
//    }

    @GetMapping("/info/{id}")
    public ResponseEntity<AlbumResponse> getAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAlbumById(id));
    }

    @GetMapping("/infoAll/{id}")
    public ResponseEntity<List<AlbumResponse>> getAllAlbumsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAllAlbumsByArtistId(id));
    }

    @GetMapping("/saved-albums")
    public ResponseEntity<List<AlbumResponse>> getAllUserSavedAlbums() {
        return ResponseEntity.ok(albumService.getCurrentUserSavedAlbums());
    }

    @GetMapping("/saved-album-check/{id}")
    public ResponseEntity<Boolean> checkFollowed(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.isSavedAlbum(id));
    }
}
