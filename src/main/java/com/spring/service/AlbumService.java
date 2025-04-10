package com.spring.service;

import com.spring.dto.request.music.artist.AddSongRequest;
import com.spring.dto.request.music.artist.AlbumRequest;
import com.spring.dto.request.music.artist.PlaylistRequest;
import com.spring.dto.request.music.artist.RemoveSongRequest;
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
    ApiResponse addSongToAlbum(AddSongRequest addSongRequest);
    ApiResponse addListSongToAlbum(AddSongRequest addSongRequest);
    ApiResponse removeSongFromAlbum(RemoveSongRequest removeSongRequest);
    ApiResponse removeListSongFromAlbum(RemoveSongRequest removeSongRequest);
    ApiResponse uploadAlbum(Long id);
}
