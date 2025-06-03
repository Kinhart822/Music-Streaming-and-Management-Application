package com.spring.repository;

import com.spring.entities.ArtistSong;
import com.spring.entities.ArtistSongId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistSongRepository extends JpaRepository<ArtistSong, ArtistSongId> {
    List<ArtistSong> findByArtistSongId_Song_Id(Long songId);
}
