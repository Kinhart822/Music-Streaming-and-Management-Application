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
    List<SongResponse> getAllSongsByArtistId(Long artistId);
    List<SongResponse> getAllAcceptedSongsByArtistId(Long artistId);
    List<SongResponse> getAllAcceptedSongsByPlaylistId(Long playlistId);
    List<SongResponse> getAllAcceptedSongsByAlbumId(Long albumId);

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

    List<SongResponse> getAcceptedSongsByArtistId();
    List<SongResponse> getAllAcceptedSongs();
    List<SongResponse> getTop10TrendingSongs();
    List<SongResponse> getTop15MostDownloadSong();

    // Upload Song Process
    ApiResponse createDraftSong(SongUploadRequest songUploadRequest);
    ApiResponse uploadSong(Long songId);     // Artist
    ApiResponse publishSong(Long id);    // Admin
    ApiResponse declineSong(Long id);    // Admin

    // Admin
    ApiResponse addSongRequest(AdminAddSongRequest request);
}
