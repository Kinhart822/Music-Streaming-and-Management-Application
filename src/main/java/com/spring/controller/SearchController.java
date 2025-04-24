package com.spring.controller;

import com.spring.dto.request.PaginationAccountRequest;
import com.spring.dto.request.PaginationGenreRequest;
import com.spring.dto.request.PaginationPlaylistAlbumRequest;
import com.spring.dto.request.PaginationSongRequest;
import com.spring.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, Object>> getPaginatedAccounts(@RequestBody PaginationAccountRequest request) {
        Map<String, Object> response = searchService.paginationAccount(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/songs")
    public ResponseEntity<Map<String, Object>> getPaginatedSongs(@RequestBody PaginationSongRequest request) {
        Map<String, Object> response = searchService.paginationSongs(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recentSongs")
    public ResponseEntity<Map<String, Object>> getPaginatedRecentSongs(@RequestBody PaginationSongRequest request) {
        Map<String, Object> response = searchService.paginationRecentSongs(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/playlists")
    public ResponseEntity<Map<String, Object>> getPaginatedPlaylists(@RequestBody PaginationPlaylistAlbumRequest request) {
        Map<String, Object> response = searchService.paginationPlaylists(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/albums")
    public ResponseEntity<Map<String, Object>> getPaginatedAlbums(@RequestBody PaginationPlaylistAlbumRequest request) {
        Map<String, Object> response = searchService.paginationAlbums(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/genres")
    public ResponseEntity<Map<String, Object>> getPaginatedGenres(@RequestBody PaginationGenreRequest request) {
        Map<String, Object> response = searchService.paginationGenres(request);
        return ResponseEntity.ok(response);
    }
}
