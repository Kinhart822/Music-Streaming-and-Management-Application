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

    Long getNumberOfListener (Long songId);
    Long getCountListen(Long songId);
    Long getNumberOfDownload(Long songId);
    Long getNumberOfUserLike(Long songId);

    Long totalSongsByArtist();
    Long totalNumberOfListeners ();
    Long totalNumberOfDownloads();
    Long totalNumberOfLikes();
    Long totalNumberOfUserFollowers();

    List<SongResponse> getSongsByStatus(String status);
    List<SongResponse> getSongsByStatusAndArtistId(String status);
    List<SongResponse> getSongsByArtistId();
    List<SongResponse> getTrendingSongs();
    List<SongResponse> getTop15BestSongEachGenre(Long genreId);

    // Upload Song Process
    ApiResponse createDraftSong(SongUploadRequest songUploadRequest);
    ApiResponse uploadSong(Long songId);     // Artist
    ApiResponse manageUploadSong(Long id, String manageProcess);    // Admin

    // Admin
    ApiResponse addSongRequest(AdminAddSongRequest request);
}
