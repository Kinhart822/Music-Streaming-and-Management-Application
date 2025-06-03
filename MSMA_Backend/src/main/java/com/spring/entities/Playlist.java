package com.spring.entities;

import com.spring.constants.PlaylistAndAlbumStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.ArrayList;
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

    @Column(name = "release_date")
    private Instant releaseDate;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdDate;

    @LastModifiedDate
    @Column
    private Instant lastModifiedDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PlaylistAndAlbumStatus playlistAndAlbumStatus;

    @OneToMany(mappedBy = "playlistSongId.playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistSong> playlistSongs = new ArrayList<>();

    @OneToMany(mappedBy = "artistPlaylistId.playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistPlaylist> artistPlaylists = new ArrayList<>();

    @OneToMany(mappedBy = "userSavedPlaylistId.playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSavedPlaylist> userSavedPlaylists;
}
