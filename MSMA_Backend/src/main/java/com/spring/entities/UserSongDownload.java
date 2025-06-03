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
@Table(name = "user_song_downloads")
public class UserSongDownload {
    @EmbeddedId
    private UserSongDownloadId  userSongDownloadId;

    @Column(name = "downloaded_at")
    private Instant downloadedAt;
}
