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
@Table(name = "songs")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title")
    private String title;

    @Column(name = "release_date")
    private Date releaseDate;

    @Column(name = "lyrics", columnDefinition = "text")
    private String lyrics;

    @Column(name = "duration")
    private String duration;

    @Column(name = "art_small_url")
    private String artSmallUrl;

    @Column(name = "art_medium_url")
    private String artMediumUrl;

    @Column(name = "art_big_url")
    private String artBigUrl;

    @Column(name = "download_permission")
    private Boolean downloadPermission;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "count_listen")
    private Long countListen;

    @Column(name = "track_url", columnDefinition = "text")
    private String trackUrl;

    @OneToMany(mappedBy = "artistSongId.song", cascade = CascadeType.ALL)
    private List<ArtistSong> artistSongs;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL)
    private List<HistoryListen> historyListens;

    @OneToMany(mappedBy = "albumSongId.song", cascade = CascadeType.ALL)
    private List<AlbumSong> albumSongs;

    @OneToMany(mappedBy = "genreSongId.song", cascade = CascadeType.ALL)
    private List<GenreSong> genreSongs;
}
