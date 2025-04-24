package com.spring.repository;

import com.spring.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistSongRepository extends JpaRepository<ArtistSong, ArtistSongId> {
    List<ArtistSong> findByArtistSongId_Song_Id(Long songId);

}
