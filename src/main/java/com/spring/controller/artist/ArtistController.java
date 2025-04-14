package com.spring.controller.artist;

import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.SongResponse;
import com.spring.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/song/genre/{genreId}")
    public ResponseEntity<List<SongResponse>> getSongsByGenre(@PathVariable Long genreId) {
        return ResponseEntity.ok(songService.getSongsByGenre(genreId));
    }

    @GetMapping("/song/listeners/{songId}")
    public ResponseEntity<ApiResponse> getNumberOfListener(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfListener(songId));
    }

    @GetMapping("/song/downloads/{songId}")
    public ResponseEntity<ApiResponse> getNumberOfDownload(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfDownload(songId));
    }

    @GetMapping("/song/likes/{songId}")
    public ResponseEntity<ApiResponse> getNumberOfUserLike(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfUserLike(songId));
    }

    @GetMapping("/song/count-listen/{songId}")
    public ResponseEntity<ApiResponse> getCountListen(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getCountListen(songId));
    }
}
