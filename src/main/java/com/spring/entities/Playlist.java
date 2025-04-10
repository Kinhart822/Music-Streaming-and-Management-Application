package com.spring.entities;

import com.spring.constants.PlaylistAndAlbumStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "playlists")
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_name")
    private String playlistName;

    @Column(name = "playlist_time_length")
    private Float playlistTimeLength;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PlaylistAndAlbumStatus playlistAndAlbumStatus;

    @OneToMany(mappedBy = "playlistSongId.playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistSong> playlistSongs;

    @OneToMany(mappedBy = "artistPlaylistId.playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistPlaylist> artistPlaylists;

    @OneToMany(mappedBy = "userPlaylistId.playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPlaylist> userPlaylists;
}
