package com.spring.controller;

import com.spring.dto.request.*;
import com.spring.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/recentSongs")
    public ResponseEntity<Map<String, Object>> getPaginatedRecentSongs(@RequestBody PaginationSongRequest request) {
        Map<String, Object> response = searchService.paginationRecentSongs(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/artistSongs")
    public ResponseEntity<Map<String, Object>> getPaginatedArtistSongs(@RequestBody PaginationSongRequest request) {
        Map<String, Object> response = searchService.paginationArtistSongs(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/songs")
    public ResponseEntity<Map<String, Object>> getPaginatedSongs(@RequestBody PaginationSongRequest request) {
        Map<String, Object> response = searchService.paginationSongs(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/artistPlaylists")
    public ResponseEntity<Map<String, Object>> getPaginatedArtistPlaylists(@RequestBody PaginationPlaylistRequest request) {
        Map<String, Object> response = searchService.paginationArtistPlaylists(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/playlists")
    public ResponseEntity<Map<String, Object>> getPaginatedPlaylists(@RequestBody PaginationPlaylistRequest request) {
        Map<String, Object> response = searchService.paginationPlaylists(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/artistAlbums")
    public ResponseEntity<Map<String, Object>> getPaginatedArtistAlbums(@RequestBody PaginationAlbumRequest request) {
        Map<String, Object> response = searchService.paginationArtistAlbums(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/albums")
    public ResponseEntity<Map<String, Object>> getPaginatedAlbums(@RequestBody PaginationAlbumRequest request) {
        Map<String, Object> response = searchService.paginationAlbums(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/genres")
    public ResponseEntity<Map<String, Object>> getPaginatedGenres(@RequestBody PaginationGenreRequest request) {
        Map<String, Object> response = searchService.paginationGenres(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recentContents")
    public ResponseEntity<Map<String, Object>> getPaginatedRecentContents(@RequestBody PaginationContentRequest request) {
        Map<String, Object> response = searchService.paginationRecentContents(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/contents")
    public ResponseEntity<Map<String, Object>> getContents(
            @RequestParam(required = false, name = "title") String title,
            @RequestParam(required = false, name = "genreId") Long genreId,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, name = "limit", defaultValue = "10") Integer limit,
            @RequestParam(required = false, name = "offset", defaultValue = "0") Integer offset
    ) {
        Map<String, Object> response = searchService.getContents(title, genreId, type, limit, offset);
        return ResponseEntity.ok(response);
    }
}
