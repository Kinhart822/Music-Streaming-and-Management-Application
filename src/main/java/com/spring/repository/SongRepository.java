package com.spring.repository;

import com.spring.constants.SongStatus;
import com.spring.entities.Song;
import com.spring.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    List<Song> findByArtistIdAndStatus(@Param("artistId") Long artistId, SongStatus status);

    @Query("SELECT usl.userSongLikeId.song FROM UserSongLike usl WHERE usl.userSongLikeId.user = :user")
    List<Song> findByUser(@Param("user") User user);

    @Query("""
                SELECT s FROM Song s
                LEFT JOIN GenreSong gs ON gs.genreSongId.song = s
                WHERE (:search IS NULL OR LOWER(s.title) LIKE %:search%)
                AND (:genreId IS NULL OR gs.genreSongId.genre.id = :genreId)
            """)
    Page<Song> findSongsByFilter(Pageable pageable, String search, Long genreId);

    @Query(value = """
                SELECT s FROM Song s
                LEFT JOIN GenreSong gs ON gs.genreSongId.song = s
                LEFT JOIN UserSongCount usc ON usc.userSongCountId.song = s
                WHERE (:search IS NULL OR LOWER(s.title) LIKE %:search%)
                AND (:genreId IS NULL OR gs.genreSongId.genre.id = :genreId)
                GROUP BY s
                ORDER BY SUM(CASE WHEN usc.countListen IS NULL THEN 0 ELSE usc.countListen END) DESC
            """)
    Page<Song> findSongsOrderByListenCount(Pageable pageable, String search, Long genreId, @Param("order") String order);
}
