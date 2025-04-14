package com.spring.service;

import com.spring.dto.request.music.admin.AdminAddSongRequest;
import com.spring.dto.request.music.artist.EditSongRequest;
import com.spring.dto.request.music.artist.SongUploadRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.SongResponse;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SongService {
    // Artist
    ApiResponse updateSong(Long id, EditSongRequest editSongRequest);
    ApiResponse deleteSong(Long id);
    List<SongResponse> getAllSongs();
    SongResponse getSongById(Long songId);
    List<SongResponse> getSongsByGenre(Long genreId);
    ApiResponse getNumberOfListener (Long songId);
    ApiResponse getCountListen(Long songId);
    List<SongResponse> getSongsByStatus(String status);
    List<SongResponse> getSongsByStatusAndArtistId(String status);
    List<SongResponse> getSongsByArtistId();
    ApiResponse getNumberOfDownload(Long songId);
    List<SongResponse> getTrendingSongs();
    List<SongResponse> getTop15BestSongEachGenre(Long genreId);
    ApiResponse getNumberOfUserLike(Long songId);

    // Upload Song Process
    ApiResponse uploadSong(SongUploadRequest songUploadRequest);     // Artist
    ApiResponse manageUploadSong(Long id, String manageProcess);    // Admin

    // Admin
    ApiResponse addSongRequest(AdminAddSongRequest request);
}
