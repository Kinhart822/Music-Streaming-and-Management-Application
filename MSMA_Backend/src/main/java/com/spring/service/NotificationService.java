package com.spring.service;

public interface NotificationService {
    // Artist notifications
    void notifyArtistSongAccepted(Long artistId, String songTitle);
    void notifyArtistSongRejected(Long artistId, String songTitle, String reason);
    void notifyArtistSongDeclined(Long artistId, String songTitle);
    void notifyArtistPlaylistAccepted(Long artistId, String playlistTitle);
    void notifyArtistPlaylistDeclined(Long artistId, String playlistTitle);
    void notifyArtistAlbumAccepted(Long artistId, String albumTitle);
    void notifyArtistAlbumDeclined(Long artistId, String albumTitle);
    void notifyArtistSongMilestone(Long artistId, String songTitle, Long streams, String milestoneType);

    // User notifications
    void notifyUserNewSong(Long userId, Long artistId, String songTitle);
    void notifyUserNewPlaylist(Long userId, Long artistId, String playlistTitle);
    void notifyUserNewAlbum(Long userId, Long artistId, String albumTitle);
}
