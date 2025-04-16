package com.spring.service;

import com.spring.dto.request.music.AdminAddSongRequest;
import com.spring.dto.request.music.EditSongRequest;
import com.spring.dto.request.music.SongUploadRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.SongResponse;

import java.util.List;

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
