package com.spring.repository;

import com.spring.entities.AlbumSong;
import com.spring.entities.AlbumSongId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumSongRepository extends JpaRepository<AlbumSong, AlbumSongId> {
}
