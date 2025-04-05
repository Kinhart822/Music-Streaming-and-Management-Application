package com.spring.repository;

import com.spring.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayListSongRepository extends JpaRepository<PlaylistSong, PlaylistSongId> {
    @Query("DELETE FROM PlaylistSong ps WHERE ps.playListSongId.playList.id = :playlistId")
    void deleteByPlaylistId(@Param("playlistId") Long playlistId);
}
