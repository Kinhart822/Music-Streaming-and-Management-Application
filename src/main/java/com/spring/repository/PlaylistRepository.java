package com.spring.repository;

import com.spring.entities.Playlist;
import com.spring.entities.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    @Query("SELECT p FROM Playlist p WHERE LOWER(p.playlistName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Playlist> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT asg.artistPlaylistId.playlist FROM ArtistPlaylist asg WHERE asg.artistPlaylistId.artist.id = :artistId")
    List<Playlist> findByArtistId(@Param("artistId") Long artistId);
}
