package com.spring.repository;

import com.spring.entities.Album;
import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Integer> {
    @Query("SELECT aa.artistAlbumId.album FROM ArtistAlbum aa WHERE aa.artistAlbumId.artist = :artist")
    List<Album> findByArtist(@Param("artist") User artist);
}
