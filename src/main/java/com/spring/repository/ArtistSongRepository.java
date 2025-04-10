package com.spring.repository;

import com.spring.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistSongRepository extends JpaRepository<ArtistSong, ArtistSongId> {
    @Modifying
    @Query("DELETE FROM ArtistSong ars WHERE ars.artistSongId.song.id = :songId")
    void deleteAllBySongId(@Param("songId") Long songId);
}
