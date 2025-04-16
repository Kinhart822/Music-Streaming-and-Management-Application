package com.spring.service;

import com.spring.dto.response.ArtistPresentation;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.SongResponse;

import java.util.List;

public interface LikeFollowingDownloadService {
    ApiResponse userLikeSong(Long id);
    ApiResponse userUnlikeSong(Long id);
    List<SongResponse> getCurrentUserLikedSongs();
    ApiResponse userFollowingArtist(Long id);
    ApiResponse userUnfollowingArtist(Long id);
    List<ArtistPresentation> getCurrentUserFollowedArtists();
    ApiResponse userDownloadSong(Long id);
    ApiResponse userUndownloadSong (Long id);
    List<String> getUserDownloadedSongs();

}
