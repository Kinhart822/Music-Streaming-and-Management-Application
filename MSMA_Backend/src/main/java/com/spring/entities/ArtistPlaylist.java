package com.spring.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "artist_playlists")
public class ArtistPlaylist {
    @EmbeddedId
    private ArtistPlaylistId artistPlaylistId;
}
