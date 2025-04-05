package com.spring.repository;

import com.spring.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumSongRepository extends JpaRepository<AlbumSong, AlbumSongId> {
}
