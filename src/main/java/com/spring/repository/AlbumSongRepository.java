package com.spring.repository;

import com.spring.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlbumSongRepository extends JpaRepository<AlbumSong, AlbumSongId> {
    @Modifying
    @Query("DELETE FROM AlbumSong als WHERE als.albumSongId.song.id = :songId")
    void deleteAllBySongId(@Param("songId") Long songId);
}
