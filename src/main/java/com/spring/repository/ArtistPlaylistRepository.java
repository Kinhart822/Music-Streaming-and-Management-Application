package com.spring.repository;

import com.spring.entities.ArtistPlaylist;
import com.spring.entities.ArtistPlaylistId;
import com.spring.entities.Playlist;
import com.spring.entities.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistPlaylistRepository extends JpaRepository<ArtistPlaylist, ArtistPlaylistId> {
    List<ArtistPlaylist> findByArtistPlaylistId_Playlist_Id(Long playlistId);
}
