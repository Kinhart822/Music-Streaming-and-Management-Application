package com.spring.controller.admin;

import com.spring.dto.response.ApiResponse;
import com.spring.service.AccountService;
import com.spring.service.AlbumService;
import com.spring.service.PlaylistService;
import com.spring.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/manage")
@RequiredArgsConstructor
public class AdminController {
    private final SongService songService;
    private final PlaylistService playlistService;
    private final AlbumService albumService;
    private final AccountService accountService;

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
}
