package com.spring.repository;

import com.spring.entities.UserSongDownload;
import com.spring.entities.UserSongDownloadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSongDownloadRepository extends JpaRepository<UserSongDownload, UserSongDownloadId> {
    @Query("SELECT COUNT(DISTINCT usc.userSongDownloadId.user.id) FROM UserSongDownload usc WHERE usc.userSongDownloadId.song.id = :songId AND usc.userSongDownloadId.user.userType = 'USER'")
    Long countDistinctUsersBySongId(@Param("songId") Long songId);

    @Query("SELECT COUNT(DISTINCT usc.userSongDownloadId.user.id) FROM UserSongDownload usc WHERE usc.userSongDownloadId.song.id IN :songIds AND usc.userSongDownloadId.user.userType = 'USER'")
    Long countDistinctListenersBySongIds(@Param("songIds") List<Long> songIds);

    @Query("SELECT u FROM UserSongDownload u WHERE u.userSongDownloadId.user.id = :userId")
    List<UserSongDownload> getAllUserDownload(@Param("userId") Long userId);
}

