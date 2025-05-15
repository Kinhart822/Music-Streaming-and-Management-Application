package com.spring.controller.artist;

import com.spring.dto.request.music.EditSongRequest;
import com.spring.dto.request.music.SongUploadRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/artist/song")
@RequiredArgsConstructor
public class ArtistSongController {
    private final SongService songService;

    @PostMapping("/createDraft")
    public ResponseEntity<ApiResponse> createDraftSong(@ModelAttribute @Valid SongUploadRequest songUploadRequest) {
        return ResponseEntity.ok(songService.createDraftSong(songUploadRequest));
    }

    @PostMapping("/upload/{id}")
    public ResponseEntity<ApiResponse> uploadSong(@PathVariable Long id) {
        return ResponseEntity.ok(songService.uploadSong(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @ModelAttribute @Valid EditSongRequest editSongRequest) {
        return ResponseEntity.ok(songService.updateSong(id, editSongRequest));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(songService.deleteSong(id));
    }
}

