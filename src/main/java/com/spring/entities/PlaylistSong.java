package com.spring.entities;

import lombok.*;
import jakarta.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "playlist_songs")
public class PlaylistSong {
    @EmbeddedId
    private PlaylistSongId playListSongId;
}
