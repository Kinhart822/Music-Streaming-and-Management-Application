package com.spring.service;

import com.spring.dto.request.PaginationAccountRequest;
import com.spring.dto.request.PaginationGenreRequest;
import com.spring.dto.request.PaginationPlaylistAlbumRequest;
import com.spring.dto.request.PaginationSongRequest;

import java.util.Map;

public interface SearchService {
    Map<String, Object> paginationAccount(PaginationAccountRequest request);
    Map<String, Object> paginationRecentSongs(PaginationSongRequest request);
    Map<String, Object> paginationSongs(PaginationSongRequest request);
    Map<String, Object> paginationPlaylists(PaginationPlaylistAlbumRequest request);
    Map<String, Object> paginationAlbums(PaginationPlaylistAlbumRequest request);
    Map<String, Object> paginationGenres(PaginationGenreRequest request);
}
