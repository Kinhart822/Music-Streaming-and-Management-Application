package com.spring.service;

import com.spring.dto.request.*;

import java.util.Map;

public interface SearchService {
    Map<String, Object> paginationAccount(PaginationAccountRequest request);
    Map<String, Object> paginationRecentSongs(PaginationSongRequest request);
    Map<String, Object> paginationArtistSongs(PaginationSongRequest request);
    Map<String, Object> paginationSongs(PaginationSongRequest request);
    Map<String, Object> paginationArtistPlaylists(PaginationPlaylistRequest request);
    Map<String, Object> paginationPlaylists(PaginationPlaylistRequest request);
    Map<String, Object> paginationArtistAlbums(PaginationAlbumRequest request);
    Map<String, Object> paginationAlbums(PaginationAlbumRequest request);
    Map<String, Object> paginationGenres(PaginationGenreRequest request);
    Map<String, Object> paginationRecentContents(PaginationContentRequest request);
    Map<String, Object> getContents(String title, Long genreId, String type, Integer limit, Integer offset);
}
