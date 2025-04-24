package com.spring.controller.artist;

import com.spring.dto.response.ArtistPresentation;
import com.spring.dto.response.GenreResponse;
import com.spring.dto.response.SongResponse;
import com.spring.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/artist")
@RequiredArgsConstructor
public class ArtistController {
    private final SongService songService;
    private final PlaylistService playlistService;
    private final AlbumService albumService;
    private final AccountService accountService;
    private final GenreService genreService;

    @GetMapping("/song/allSong")
    public ResponseEntity<List<SongResponse>> getSongsByArtist() {
        List<SongResponse> songs = songService.getSongsByArtistId();
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/genre/allGenres")
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        List<GenreResponse> genres = genreService.getAllGenres();
        return ResponseEntity.ok(genres);
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
    public ResponseEntity<Long> getNumberOfListener(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfListener(songId));
    }

    @GetMapping("/song/downloads/{songId}")
    public ResponseEntity<Long> getNumberOfDownload(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfDownload(songId));
    }

    @GetMapping("/song/likes/{songId}")
    public ResponseEntity<Long> getNumberOfUserLike(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getNumberOfUserLike(songId));
    }

    @GetMapping("/song/count-listen/{songId}")
    public ResponseEntity<Long> getCountListen(@PathVariable Long songId) {
        return ResponseEntity.ok(songService.getCountListen(songId));
    }

    @GetMapping("/otherArtists")
    public ResponseEntity<List<ArtistPresentation>> getAllOtherArtist() {
        return ResponseEntity.ok(accountService.getAllOtherArtist());
    }

    @GetMapping("/totalSongs")
    public ResponseEntity<Long> getTotalSongsByArtist() {
        return ResponseEntity.ok(songService.totalSongsByArtist());
    }

    @GetMapping("/totalPlaylists")
    public ResponseEntity<Long> getTotalPlaylistsByArtist() {
        return ResponseEntity.ok(playlistService.totalArtistPlaylist());
    }

    @GetMapping("/totalAlbums")
    public ResponseEntity<Long> getTotalAlbumsByArtist() {
        return ResponseEntity.ok(albumService.totalAlbum());
    }

    @GetMapping("/totalFollowers")
    public ResponseEntity<Long> getTotalNumberOfFollowersByArtist() {
        return ResponseEntity.ok(songService.totalNumberOfUserFollowers());
    }

    @GetMapping("/totalListeners")
    public ResponseEntity<Long> getTotalNumberOfListenersByArtist() {
        return ResponseEntity.ok(songService.totalNumberOfListeners());
    }

    @GetMapping("/totalLikes")
    public ResponseEntity<Long> getNumberOfUserLikesByArtist() {
        return ResponseEntity.ok(songService.totalNumberOfLikes());
    }

    @GetMapping("/totalDownloads")
    public ResponseEntity<Long> getTotalNumberOfDownloadsByArtist() {
        return ResponseEntity.ok(songService.totalNumberOfDownloads());
    }
}
