package com.spring.repository;

import com.spring.entities.UserSavedAlbum;
import com.spring.entities.UserSavedAlbumId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSavedAlbumRepository extends JpaRepository<UserSavedAlbum, UserSavedAlbumId> {
    @Query("""
                SELECT CASE WHEN COUNT(usa) > 0 THEN true ELSE false END
                FROM UserSavedAlbum usa
                WHERE usa.userSavedAlbumId.album.id = :albumId AND usa.userSavedAlbumId.user.id = :userId
            """)
    boolean existsByUserIdAndAlbumId(@Param("userId") Long userId, @Param("albumId") Long albumId);
}
