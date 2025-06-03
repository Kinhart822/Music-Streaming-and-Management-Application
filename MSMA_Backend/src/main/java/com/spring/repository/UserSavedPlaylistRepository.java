package com.spring.repository;

import com.spring.entities.UserSavedPlaylist;
import com.spring.entities.UserSavedPlaylistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSavedPlaylistRepository extends JpaRepository<UserSavedPlaylist, UserSavedPlaylistId> {
    @Query("""
                SELECT CASE WHEN COUNT(usp) > 0 THEN true ELSE false END
                FROM UserSavedPlaylist usp
                WHERE usp.userSavedPlaylistId.playlist.id = :playlistId AND usp.userSavedPlaylistId.user.id = :userId
            """)
    boolean existsByUserIdAndPlaylistId(@Param("userId") Long userId, @Param("playlistId") Long playlistId);
}
