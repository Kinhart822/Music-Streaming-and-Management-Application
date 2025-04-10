package com.spring.controller.artist;

import com.spring.dto.response.SongResponse;
import com.spring.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/artist")
@RequiredArgsConstructor
public class ArtistController {
    private final SongService songService;

    @GetMapping("/song/allSong")
    public ResponseEntity<List<SongResponse>> getSongsByArtist() {
        List<SongResponse> songs = songService.getSongsByArtistId();
        return ResponseEntity.ok(songs);
    }

    // Lấy bài hát theo status (ví dụ: ACCEPTED, PENDING)
    @GetMapping("/song/allSongByStatus")
    public ResponseEntity<List<SongResponse>> getSongsByStatus(
            @RequestParam String status
    ) {
        List<SongResponse> songs = songService.getSongsByStatusAndArtistId(status);
        return ResponseEntity.ok(songs);
    }
}
