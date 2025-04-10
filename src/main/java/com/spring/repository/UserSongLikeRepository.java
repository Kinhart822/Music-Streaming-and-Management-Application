package com.spring.repository;

import com.spring.entities.UserSongLike;
import com.spring.entities.UserSongLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSongLikeRepository extends JpaRepository<UserSongLike, UserSongLikeId> {
    @Query("SELECT COUNT(u) FROM UserSongLike u WHERE u.userSongLikeId.song.id = :songId")
    Long countBySongId(@Param("songId") Long songId);
}

