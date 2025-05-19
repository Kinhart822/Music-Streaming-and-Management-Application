package com.spring.service;

import com.spring.dto.response.ArtistPresentation;
import com.spring.dto.response.ApiResponse;
import com.spring.dto.response.SongResponse;

import java.util.List;

public interface LikeFollowingDownloadService {
    // Favorite Songs
    ApiResponse userLikeSong(Long id);
    ApiResponse userUnlikeSong(Long id);
    Boolean isFavoriteSong(Long id);
    List<SongResponse> getCurrentUserLikedSongs();

    // Favorite Artists
    ApiResponse userFollowingArtist(Long id);
    ApiResponse userUnfollowingArtist(Long id);
    Boolean isFollowedArtist(Long id);
    List<ArtistPresentation> getCurrentUserFollowedArtists();

    // Download Songs
    ApiResponse userDownloadSong(Long id);
    ApiResponse userUndownloadSong (Long id);
    List<String> getUserDownloadedSongs();
}
