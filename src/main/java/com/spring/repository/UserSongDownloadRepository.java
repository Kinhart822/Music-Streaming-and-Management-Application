package com.spring.repository;

import com.spring.entities.UserSongDownload;
import com.spring.entities.UserSongDownloadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSongDownloadRepository extends JpaRepository<UserSongDownload, UserSongDownloadId> {
    @Query("SELECT COUNT(u) FROM UserSongDownload u WHERE u.userSongDownloadId.song.id = :songId")
    Long countBySongId(@Param("songId") Long songId);

    @Modifying
    @Query("DELETE FROM UserSongDownload als WHERE als.userSongDownloadId.song.id = :songId")
    void deleteAllBySongId(@Param("songId") Long songId);
}

