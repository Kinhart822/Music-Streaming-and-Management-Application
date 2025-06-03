package com.spring.repository;

import com.spring.entities.ArtistAlbum;
import com.spring.entities.ArtistAlbumId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistAlbumRepository extends JpaRepository<ArtistAlbum, ArtistAlbumId> {
    List<ArtistAlbum> findByArtistAlbumId_Album_Id(Long playlistId);
}
