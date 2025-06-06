package com.spring.entities;

import com.spring.constants.SongStatus;
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
@Table(name = "songs")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "release_date")
    private Instant releaseDate;

    @Column(name = "lyrics", columnDefinition = "text")
    private String lyrics;

    @Column(name = "duration")
    private String duration;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;    // Song image url from Cloudinary

    @Column(name = "download_permission")
    private Boolean downloadPermission;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "mp3_url", columnDefinition = "text")
    private String mp3Url;        // Song file url from Cloudinary

    @Column(name = "count_listener")
    private Long countListener;     // Số người nghe

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SongStatus songStatus;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdDate;

    @LastModifiedDate
    @Column
    private Instant lastModifiedDate;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoryListen> historyListens;

    @OneToMany(mappedBy = "artistSongId.song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistSong> artistSongs = new ArrayList<>();

    @OneToMany(mappedBy = "playlistSongId.song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistSong> playlistSongs = new ArrayList<>();

    @OneToMany(mappedBy = "albumSongId.song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlbumSong> albumSongs = new ArrayList<>();

    @OneToMany(mappedBy = "genreSongId.song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GenreSong> genreSongs = new ArrayList<>();

    @OneToMany(mappedBy = "userSongLikeId.song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSongLike> userSongLikes;

    @OneToMany(mappedBy = "userSongDownloadId.song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSongDownload> userSongDownloads;

    @OneToMany(mappedBy = "userSongCountId.song", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSongCount> userSongCounts;
}
