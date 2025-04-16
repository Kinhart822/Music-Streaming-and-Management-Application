package com.spring.entities;

import com.spring.constants.PlaylistAndAlbumStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "albums")
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "album_name")
    private String albumName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "release_date")
    private Date releaseDate;

    @Column(name = "album_time_length")
    private Float albumTimeLength;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PlaylistAndAlbumStatus playlistAndAlbumStatus;

    @OneToMany(mappedBy = "artistAlbumId.album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistAlbum> artistAlbums;

    @OneToMany(mappedBy = "albumSongId.album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlbumSong> albumSongs;

    @OneToMany(mappedBy = "userAlbumId.album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAlbum> userAlbums;

    @OneToMany(mappedBy = "userSavedAlbumId.album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSavedAlbum> userSavedAlbums;
}
