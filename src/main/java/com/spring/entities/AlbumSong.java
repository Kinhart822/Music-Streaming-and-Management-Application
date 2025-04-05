package com.spring.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "album_songs")
public class AlbumSong {
    @EmbeddedId
    private AlbumSongId albumSongId;
}
