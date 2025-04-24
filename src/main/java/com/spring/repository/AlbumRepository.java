package com.spring.repository;

import com.spring.entities.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    @Query("SELECT asg.artistAlbumId.album FROM ArtistAlbum asg WHERE asg.artistAlbumId.artist.id = :artistId")
    List<Album> findByArtistId(@Param("artistId") Long artistId);

    @Query("SELECT a FROM Album a WHERE LOWER(a.albumName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Album> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

}
