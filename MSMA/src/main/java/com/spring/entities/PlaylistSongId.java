package com.spring.entities;

import lombok.*;
import jakarta.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class PlaylistSongId implements Serializable {
    @ManyToOne
    @JoinColumn(name = "song_id")
    private Song song;

    @ManyToOne
    @JoinColumn(name = "playlist_id")
    private Playlist playList;
}
