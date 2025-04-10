package com.spring.repository;

import com.spring.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayListSongRepository extends JpaRepository<PlaylistSong, PlaylistSongId> {
}
