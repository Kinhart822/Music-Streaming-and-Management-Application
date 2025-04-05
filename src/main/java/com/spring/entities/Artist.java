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

    @Column(name = "image", columnDefinition = "varchar(500)")
    private String image;

    @Column(name = "count_listen")
    private Long countListen;

    @OneToMany(mappedBy = "artistSongId.artist")
    private List<ArtistSong> artistSongs;

    @OneToMany(mappedBy = "artistAlbumId.artist")
    private List<ArtistAlbum> artistAlbums;
}
