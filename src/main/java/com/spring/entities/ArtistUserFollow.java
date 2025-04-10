package com.spring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "artist_user_follows")
public class ArtistUserFollow {
    @EmbeddedId
    private ArtistUserFollowId artistUserFollowId;

    @Column(name = "followed_at")
    private Instant followedAt;
}
