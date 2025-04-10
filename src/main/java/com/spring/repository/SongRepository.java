package com.spring.repository;

import com.spring.constants.SongStatus;
import com.spring.entities.GenreSong;
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

    List<Song> findAllBySongStatus(SongStatus songStatus);

    @Query("SELECT COUNT(a) FROM Song a")
    Long countAllSongs();

    @Query("SELECT asg.artistSongId.song FROM ArtistSong asg WHERE asg.artistSongId.artist.id = :artistId")
    List<Song> findByArtistId(@Param("artistId") Long artistId);

    @Query("SELECT asg.artistSongId.song FROM ArtistSong asg WHERE asg.artistSongId.artist.id = :artistId")
    List<Song> findByArtistIdAndStatus(@Param("artistId")Long artistId, SongStatus status);
}
