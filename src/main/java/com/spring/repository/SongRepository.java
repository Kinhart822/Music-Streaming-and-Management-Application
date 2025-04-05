package com.spring.repository;

import com.spring.entities.Song;
import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
    @Query("SELECT asg.artistSongId.song FROM ArtistSong asg WHERE asg.artistSongId.artist = :artist")
    List<Song> findByArtist(@Param("artist") User artist);
}
