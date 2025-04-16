package com.spring.entities;

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
@Table(name = "user_saved_playlists")
public class UserSavedPlaylist {
    @EmbeddedId
    private UserSavedPlaylistId userSavedPlaylistId;
}
