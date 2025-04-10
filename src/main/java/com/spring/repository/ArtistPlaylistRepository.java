package com.spring.repository;

import com.spring.entities.ArtistPlaylist;
import com.spring.entities.ArtistPlaylistId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistPlaylistRepository extends JpaRepository<ArtistPlaylist, ArtistPlaylistId> {
}
