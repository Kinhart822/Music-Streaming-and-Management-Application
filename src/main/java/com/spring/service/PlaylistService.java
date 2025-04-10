package com.spring.service;

import com.spring.dto.request.music.artist.AddSongRequest;
import com.spring.dto.request.music.artist.PlaylistRequest;
import com.spring.dto.request.music.artist.RemoveSongRequest;
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
}
