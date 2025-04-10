package com.spring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "user_song_count")
public class UserSongCount {
    @EmbeddedId
    private UserSongCountId userSongCountId;

    @Column(name = "count_listen")
    private Long countListen;

    @Column(name = "count_listener")
    private Long countListener;
}
