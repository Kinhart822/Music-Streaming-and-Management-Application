package com.spring.repository;

import com.spring.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistSongRepository extends JpaRepository<ArtistSong, ArtistSongId> {
}
