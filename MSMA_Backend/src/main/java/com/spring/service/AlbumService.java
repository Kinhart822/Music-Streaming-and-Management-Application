package com.spring.service;

import com.spring.dto.request.music.AddSongRequest;
import com.spring.dto.request.music.AdminAddAlbumRequest;
import com.spring.dto.request.music.AlbumRequest;
import com.spring.dto.request.music.RemoveSongRequest;
import com.spring.dto.response.AlbumResponse;
import com.spring.dto.response.ApiResponse;

import java.util.List;

public interface AlbumService {
    AlbumResponse createAlbum(AlbumRequest request);

    AlbumResponse updateAlbum(Long playlistId, AlbumRequest request);

    ApiResponse deleteAlbum(Long playlistId);

    AlbumResponse getAlbumById(Long id);

    List<AlbumResponse> getAllAlbumsByCurrentAccount();

    List<AlbumResponse> getAllAlbumsByArtistId(Long id);

    List<AlbumResponse> getAllAcceptedAlbumsByArtistId(Long id);

    Boolean isSavedAlbum(Long id);

    List<AlbumResponse> getCurrentUserSavedAlbums();

    List<AlbumResponse> getRecentCurrentUserSavedAlbums();

    ApiResponse addSongToAlbum(AddSongRequest addSongRequest);

    ApiResponse addListSongToAlbum(AddSongRequest addSongRequest);

    ApiResponse removeSongFromAlbum(RemoveSongRequest removeSongRequest);

    ApiResponse removeListSongFromAlbum(RemoveSongRequest removeSongRequest);

    ApiResponse uploadAlbum(Long id);

    ApiResponse publishAlbum(Long id);

    ApiResponse declineAlbum(Long id);

    ApiResponse userSaveAlbum(Long albumId);

    ApiResponse userUnSaveAlbum(Long albumId);

    Long totalAlbums();

    Long totalPendingAlbums();

    Long totalAlbumsByArtist();

    ApiResponse adminAddAlbumRequest(AdminAddAlbumRequest adminAddAlbumRequest);
}
