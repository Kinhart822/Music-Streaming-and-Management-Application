package com.spring.entities;

import lombok.*;
import jakarta.persistence.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "artists")
public class Artist extends User{
    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "count_listen")
    private Long countListen;

    @OneToMany(mappedBy = "artistSongId.artist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistSong> artistSongs;

    @OneToMany(mappedBy = "artistAlbumId.artist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistAlbum> artistAlbums;

    @OneToMany(mappedBy = "artistUserFollowId.artist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistUserFollow> artistUserFollows;

    @OneToMany(mappedBy = "artistPlaylistId.artist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistPlaylist> artistPlaylists;
}
