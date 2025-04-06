package com.spring.controller;

import com.spring.dto.request.music.artist.SongUploadRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.service.SongUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/artist/song")
@RequiredArgsConstructor
public class SongUploadController {

    private final SongUploadService songUploadService;

    @PostMapping("/uploadFile")
    public ResponseEntity<ApiResponse> uploadSong(@ModelAttribute @Valid SongUploadRequest songUploadRequest) {
        return ResponseEntity.ok(songUploadService.uploadSong(songUploadRequest));
    }
}

