package com.spring.controller.admin;

import com.spring.dto.request.account.*;
import com.spring.dto.response.*;
import com.spring.service.AccountService;
import com.spring.service.AlbumService;
import com.spring.service.PlaylistService;
import com.spring.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/manage")
@RequiredArgsConstructor
public class AdminController {
    private final SongService songService;
    private final PlaylistService playlistService;
    private final AlbumService albumService;
    private final AccountService accountService;

    /*
        TODO: Account
    */
    @PostMapping("/createAdmin")
    public ResponseEntity<ApiResponse> createAdmin(@RequestBody @Valid CreateAdmin request) {
        return ResponseEntity.ok(accountService.createAdmin(request));
    }

    @PostMapping("/createArtist")
    public ResponseEntity<ApiResponse> createArtist(@RequestBody @Valid CreateArtist request) {
        return ResponseEntity.ok(accountService.createArtist(request));
    }

    @PostMapping("/createArtist/batch")
    public ResponseEntity<ApiResponse> createArtistFromList(@RequestBody @Valid CreateArtistFromList request) {
        return ResponseEntity.ok(accountService.createArtistFromList(request));
    }

    @GetMapping("/countArtist")
    public ResponseEntity<Long> getTotalArtists() {
        return ResponseEntity.ok(accountService.countArtists());
    }

    /*
        TODO: User
    */
    @GetMapping("/countUser")
    public ResponseEntity<Long> getTotalUser() {
        return ResponseEntity.ok(accountService.countUsers());
    }

    @DeleteMapping("/deleteUser/{userId}")
    public ResponseEntity<ApiResponse> deleteAccount(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.deleteAccount(userId));
    }

    /*
        TODO: Song
    */
    @GetMapping("/countSong")
    public ResponseEntity<Long> getTotalSong() {
        return ResponseEntity.ok(accountService.countSongs());
    }

    @PostMapping("/songUpload/{id}")
    public ResponseEntity<ApiResponse> manageSongUpload(@PathVariable Long id, @RequestParam String manageProcess) {
        return ResponseEntity.ok(songService.manageUploadSong(id, manageProcess));
    }

    @PostMapping("/playlistUpload/{id}")
    public ResponseEntity<ApiResponse> managePlaylistUpload(@PathVariable Long id, @RequestParam String manageProcess) {
        return ResponseEntity.ok(playlistService.manageUploadPlaylist(id, manageProcess));
    }

    @PostMapping("/albumUpload/{id}")
    public ResponseEntity<ApiResponse> manageAlbumUpload(@PathVariable Long id, @RequestParam String manageProcess) {
        return ResponseEntity.ok(albumService.manageUploadAlbum(id, manageProcess));
    }

    @PostMapping("/processingDeleteRequest/{id}")
    public ResponseEntity<ApiResponse> processingDeleteRequest(@PathVariable Long id, @RequestParam String manageProcess) {
        return ResponseEntity.ok(accountService.processingDeleteRequest(id, manageProcess));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(songService.deleteSong(id));
    }

    /*
        TODO: Playlist
    */
    @GetMapping("/playlist/info/{id}")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getPlaylistById(id));
    }

    @GetMapping("/playlist/infoAll/{id}")
    public ResponseEntity<List<PlaylistResponse>> getAllPlaylistsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getAllPlaylistsByArtistId(id));
    }

    /*
        TODO: Album
    */
    @GetMapping("/album/info/{id}")
    public ResponseEntity<AlbumResponse> getAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAlbumById(id));
    }

    @GetMapping("/album/infoAll/{id}")
    public ResponseEntity<List<AlbumResponse>> getAllAlbumsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAllAlbumsByArtistId(id));
    }

    /*
        TODO: Status Delete
    */
    @GetMapping("/status/infoAllArtist")
    public ResponseEntity<List<ArtistPresentation>> getInfoAllArtist() {
        return ResponseEntity.ok(accountService.getAllArtistByInactiveStatus());
    }

    @GetMapping("/status/infoAllAdmin")
    public ResponseEntity<List<AdminPresentation>> getInfoAllAdmin() {
        return ResponseEntity.ok(accountService.getAllAdminByLockedStatus());
    }
}
