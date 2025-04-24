package com.spring.repository;

import com.spring.entities.UserSongLike;
import com.spring.entities.UserSongLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSongLikeRepository extends JpaRepository<UserSongLike, UserSongLikeId> {
    @Query("SELECT COUNT(DISTINCT usc.userSongLikeId.user.id) FROM UserSongLike usc WHERE usc.userSongLikeId.song.id = :songId AND usc.userSongLikeId.user.userType = 'USER'")
    Long countDistinctUsersBySongId(@Param("songId") Long songId);

    @Query("SELECT COUNT(DISTINCT usc.userSongLikeId.user.id) FROM UserSongLike usc WHERE usc.userSongLikeId.song.id IN :songIds AND usc.userSongLikeId.user.userType = 'USER'")
    Long countDistinctListenersBySongIds(@Param("songIds") List<Long> songIds);
}

