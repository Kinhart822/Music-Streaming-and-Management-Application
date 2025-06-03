package com.spring.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "artist_albums")
public class ArtistAlbum {
    @EmbeddedId
    private ArtistAlbumId artistAlbumId;
}
