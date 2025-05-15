package com.spring.service;

import com.spring.dto.request.music.AddSongRequest;
import com.spring.dto.request.music.AdminAddPlaylistRequest;
import com.spring.dto.request.music.PlaylistRequest;
import com.spring.dto.request.music.RemoveSongRequest;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.PlaylistResponse;

import java.util.List;

public interface PlaylistService {
    PlaylistResponse createPlaylist(PlaylistRequest request);
    PlaylistResponse updatePlaylist(Long playlistId, PlaylistRequest request);
    ApiResponse deletePlaylist(Long playlistId);
    PlaylistResponse getPlaylistById(Long id);
    List<PlaylistResponse> getAllPlaylistsByCurrentAccount();
    List<PlaylistResponse> getAllPlaylistsByArtistId(Long id);

    ApiResponse addSongToPlaylist(AddSongRequest addSongRequest);
    ApiResponse addListSongToPlaylist(AddSongRequest addSongRequest);
    ApiResponse removeSongFromPlaylist(RemoveSongRequest removeSongRequest);
    ApiResponse removeListSongFromPlaylist(RemoveSongRequest removeSongRequest);

    ApiResponse uploadPlaylist(Long id);
    ApiResponse publishPlaylist(Long id);
    ApiResponse declinePlaylist(Long id);

    ApiResponse userSavePlaylist(Long playlistId);

    Long totalPlaylists();
    Long totalPendingPlaylists();
    Long totalArtistPlaylists();

    ApiResponse adminAddPlaylistRequest(AdminAddPlaylistRequest adminAddPlaylistRequest);
}
