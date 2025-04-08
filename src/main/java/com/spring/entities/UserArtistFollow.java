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
@Table(name = "user_artist_follows")
public class UserArtistFollow {
    @EmbeddedId
    private UserArtistFollowId  userArtistFollowId;

    @Column(name = "followed_at")
    private Instant followedAt;
}
