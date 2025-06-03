package com.spring.controller.admin;

import com.spring.dto.request.account.*;
import com.spring.dto.request.music.*;
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
public class AdminManagementController {
    private final SongService songService;
    private final PlaylistService playlistService;
    private final AlbumService albumService;
    private final AccountService accountService;

    /*
        TODO: Account
    */
    @PostMapping("/createUser")
    public ResponseEntity<ApiResponse> createUser(@RequestBody @Valid CreateUser request) {
        return ResponseEntity.ok(accountService.createUser(request));
    }

    @PostMapping("/createAdmin")
    public ResponseEntity<ApiResponse> createAdmin(@RequestBody @Valid CreateAdmin request) {
        return ResponseEntity.ok(accountService.createAdmin(request));
    }

    @PostMapping("/createArtist")
    public ResponseEntity<ApiResponse> createArtist(@RequestBody @Valid CreateArtist request) {
        return ResponseEntity.ok(accountService.createArtist(request));
    }

    @PostMapping("/processingDeleteRequest/{id}")
    public ResponseEntity<ApiResponse> processingDeleteRequest(@PathVariable Long id, @RequestParam String manageProcess) {
        return ResponseEntity.ok(accountService.processingDeleteRequest(id, manageProcess));
    }

    @GetMapping("/countArtist")
    public ResponseEntity<Long> getTotalArtists() {
        return ResponseEntity.ok(accountService.countArtists());
    }

    @GetMapping("/allActiveArtists")
    public ResponseEntity<List<ArtistPresentation>> getAllActiveArtists() {
        return ResponseEntity.ok(accountService.getAllActiveArtists());
    }

    /*
        TODO: User
    */
    @GetMapping("/countUser")
    public ResponseEntity<Long> getTotalUser() {
        return ResponseEntity.ok(accountService.countUsers());
    }

    @DeleteMapping("/adminDeleteUser/{userId}")
    public ResponseEntity<ApiResponse> deleteAccount(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.adminDeleteAccount(userId));
    }

    /*
        TODO: Song
    */
    @GetMapping("/countSong")
    public ResponseEntity<Long> getTotalSongs() {
        return ResponseEntity.ok(accountService.countSongs());
    }

    @GetMapping("/countPendingSong")
    public ResponseEntity<Long> getTotalPendingSongs() {
        return ResponseEntity.ok(accountService.countPendingSongs());
    }

    @GetMapping("/song/allAcceptedSong")
    public ResponseEntity<List<SongResponse>> getAllAcceptedSongs() {
        List<SongResponse> songs = songService.getAllAcceptedSongs();
        return ResponseEntity.ok(songs);
    }

    @PostMapping("/song/publish/{id}")
    public ResponseEntity<ApiResponse> publishSong(@PathVariable Long id) {
        return ResponseEntity.ok(songService.publishSong(id));
    }

    @PostMapping("/song/decline/{id}")
    public ResponseEntity<ApiResponse> declineSong(@PathVariable Long id) {
        return ResponseEntity.ok(songService.declineSong(id));
    }

    @PostMapping("/song/create")
    public ResponseEntity<ApiResponse> addSong(@ModelAttribute AdminAddSongRequest addSongRequest) {
        return ResponseEntity.ok(songService.addSongRequest(addSongRequest));
    }

    @PutMapping("/song/update/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @ModelAttribute @Valid EditSongRequest editSongRequest) {
        return ResponseEntity.ok(songService.updateSong(id, editSongRequest));
    }

    @DeleteMapping("/song/delete/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(songService.deleteSong(id));
    }

    /*
        TODO: Playlist
    */
    @GetMapping("/countPlaylist")
    public ResponseEntity<Long> getTotalPlaylists() {
        return ResponseEntity.ok(playlistService.totalPlaylists());
    }

    @GetMapping("/countPendingPlaylist")
    public ResponseEntity<Long> getTotalPendingPlaylists() {
        return ResponseEntity.ok(playlistService.totalPendingPlaylists());
    }

    @GetMapping("/playlist/info/{id}")
    public ResponseEntity<PlaylistResponse> getPlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getPlaylistById(id));
    }

    @GetMapping("/playlist/infoAll/{id}")
    public ResponseEntity<List<PlaylistResponse>> getAllPlaylistsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.getAllPlaylistsByArtistId(id));
    }

    @PostMapping("/playlist/create")
    public ResponseEntity<ApiResponse> addPlaylist(@ModelAttribute AdminAddPlaylistRequest adminAddPlaylistRequest) {
        return ResponseEntity.ok(playlistService.adminAddPlaylistRequest(adminAddPlaylistRequest));
    }

    @PutMapping("/playlist/update/{id}")
    public ResponseEntity<PlaylistResponse> updatePlaylist(@PathVariable Long id, @ModelAttribute @Valid PlaylistRequest playlistRequest) {
        return ResponseEntity.ok(playlistService.updatePlaylist(id, playlistRequest));
    }

    @DeleteMapping("/playlist/delete/{id}")
    public ResponseEntity<ApiResponse> deletePlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.deletePlaylist(id));
    }

    @PostMapping("/playlist/publish/{id}")
    public ResponseEntity<ApiResponse> publishPlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.publishPlaylist(id));
    }

    @PostMapping("/playlist/decline/{id}")
    public ResponseEntity<ApiResponse> declinePlaylist(@PathVariable Long id) {
        return ResponseEntity.ok(playlistService.declinePlaylist(id));
    }

    /*
        TODO: Album
    */
    @GetMapping("/countAlbum")
    public ResponseEntity<Long> getTotalAlbums() {
        return ResponseEntity.ok(albumService.totalAlbums());
    }

    @GetMapping("/countPendingAlbum")
    public ResponseEntity<Long> getTotalPendingAlbums() {
        return ResponseEntity.ok(albumService.totalPendingAlbums());
    }

    @GetMapping("/album/info/{id}")
    public ResponseEntity<AlbumResponse> getAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAlbumById(id));
    }

    @GetMapping("/album/infoAll/{id}")
    public ResponseEntity<List<AlbumResponse>> getAllAlbumsByArtistId(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.getAllAlbumsByArtistId(id));
    }

    @PostMapping("/album/create")
    public ResponseEntity<ApiResponse> addAlbum(@ModelAttribute AdminAddAlbumRequest adminAddAlbumRequest) {
        return ResponseEntity.ok(albumService.adminAddAlbumRequest(adminAddAlbumRequest));
    }

    @PutMapping("/album/update/{id}")
    public ResponseEntity<AlbumResponse> updateAlbum(@PathVariable Long id, @ModelAttribute @Valid AlbumRequest albumRequest) {
        return ResponseEntity.ok(albumService.updateAlbum(id, albumRequest));
    }

    @DeleteMapping("/album/delete/{id}")
    public ResponseEntity<ApiResponse> deleteAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.deleteAlbum(id));
    }

    @PostMapping("/album/publish/{id}")
    public ResponseEntity<ApiResponse> publishAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.publishAlbum(id));
    }

    @PostMapping("/album/decline/{id}")
    public ResponseEntity<ApiResponse> declineAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(albumService.declineAlbum(id));
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
