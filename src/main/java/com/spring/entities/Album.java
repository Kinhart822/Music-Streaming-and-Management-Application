package com.spring.entities;

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

    @Column(name = "total_listen")
    private int totalListen;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "download_permission")
    private boolean downloadPermission;

    @OneToMany(mappedBy = "artistAlbumId.album", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ArtistAlbum> artistAlbums;

    @OneToMany(mappedBy = "albumSongId.album", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AlbumSong> albumSongs;

    @Column(name = "count_listen")
    private Long countListen;
}
