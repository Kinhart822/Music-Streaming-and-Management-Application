package com.spring.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AlbumSong {
    @EmbeddedId
    private AlbumSongId albumSongId;
}
