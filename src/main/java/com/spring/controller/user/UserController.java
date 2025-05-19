package com.spring.controller.user;

import com.spring.dto.response.*;
import com.spring.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final AccountService accountService;
    private final SongService songService;
    private final PlaylistService playlistService;
    private final AlbumService albumService;
    private final UserSongCountService userSongCountService;

    @GetMapping("/viewUserProfile")
    public ResponseEntity<UserPresentation> viewAnotherUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.viewUserProfile(id));
    }

    @GetMapping("/viewArtistProfile/{id}")
    public ResponseEntity<ArtistPresentation> viewArtistProfile(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.viewArtistProfile(id));
    }

    @GetMapping("/viewArtistProfile/{id}/songs")
    public ResponseEntity<List<SongResponse>> getArtistSongs(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getAllAcceptedSongsByArtistId(id));
    }

    @GetMapping("/viewArtistProfile/{id}/playlists")
    public ResponseEntity<List<PlaylistResponse>> getAllAcceptedPlaylistsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getAllAcceptedPlaylistsByArtistId(id));
    }

    @GetMapping("/viewArtistProfile/playlist/{id}/songs")
    public ResponseEntity<List<SongResponse>> getPlaylistSongs(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getAllAcceptedSongsByPlaylistId(id));
    }

    @GetMapping("/viewArtistProfile/{id}/albums")
    public ResponseEntity<List<AlbumResponse>> getAllAcceptedAlbumsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAllAcceptedAlbumsByArtistId(id));
    }

    @GetMapping("/viewArtistProfile/album/{id}/songs")
    public ResponseEntity<List<SongResponse>> getAlbumSongs(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getAllAcceptedSongsByAlbumId(id));
    }

    @PostMapping("/listen/{songId}")
    public ResponseEntity<ApiResponse> recordSongListen(@PathVariable Long songId) {
        return ResponseEntity.ok(userSongCountService.incrementListenCount(songId));
    }
}
