package com.spring.repository;

import com.spring.entities.ArtistAlbum;
import com.spring.entities.ArtistAlbumId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistAlbumRepository extends JpaRepository<ArtistAlbum, ArtistAlbumId> {

}
