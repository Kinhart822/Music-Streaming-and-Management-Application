package com.spring.entities;

import lombok.*;
import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "artist_songs")
public class ArtistSong {
    @EmbeddedId
    private ArtistSongId artistSongId;
}
