package com.spring.repository;

import com.spring.constants.SongStatus;
import com.spring.entities.Song;
import com.spring.entities.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findAllBySongStatus(SongStatus songStatus);

    @Query("SELECT COUNT(a) FROM Song a")
    Long countAllSongs();

    @Query("SELECT COUNT(a) FROM Song a WHERE a.songStatus = 'PENDING'")
    Long countAllPendingSongs();

    @Query("SELECT asg.artistSongId.song FROM ArtistSong asg WHERE asg.artistSongId.artist.id = :artistId")
    List<Song> findByArtistId(@Param("artistId") Long artistId);

    @Query("SELECT asg.playlistSongId.song FROM PlaylistSong asg WHERE asg.playlistSongId.playlist.id = :playlistId")
    List<Song> findByPlaylistId(@Param("playlistId") Long playlistId);

    @Query("SELECT asg.albumSongId.song FROM AlbumSong asg WHERE asg.albumSongId.album.id = :albumId")
    List<Song> findByAlbumId(@Param("albumId") Long albumId);

    @Query("""
                SELECT asg.artistSongId.song\s
                FROM ArtistSong asg\s
                WHERE asg.artistSongId.artist.id = :artistId\s
                AND asg.artistSongId.song.songStatus = :status
           \s""")
    List<Song> findByArtistIdAndStatus(@Param("artistId") Long artistId, @Param("status") SongStatus status);

    @Query("SELECT usl.userSongLikeId.song FROM UserSongLike usl WHERE usl.userSongLikeId.user = :user")
    List<Song> findByUser(@Param("user") User user);

    @Query("""
            SELECT s FROM Song s
            LEFT JOIN ArtistSong asi ON asi.artistSongId.song = s
            LEFT JOIN GenreSong gs ON gs.genreSongId.song = s
            LEFT JOIN UserSongCount usc ON usc.userSongCountId.song = s
            WHERE (:search IS NULL OR LOWER(s.title) LIKE %:search%)
              AND (asi.artistSongId.artist.id = :artistId)
              AND (:genreId IS NULL OR gs.genreSongId.genre.id = :genreId)
            GROUP BY s
            """)
    Page<Song> findArtistSongsByFilter(Pageable pageable, @Param("search") String search, @Param("genreId") Long genreId, @Param("artistId") Long artistId);

    @Query("""
            SELECT s FROM Song s
            LEFT JOIN GenreSong gs ON gs.genreSongId.song = s
            LEFT JOIN UserSongCount usc ON usc.userSongCountId.song = s
            WHERE (:search IS NULL OR LOWER(s.title) LIKE %:search%)
              AND (:genreId IS NULL OR gs.genreSongId.genre.id = :genreId)
            GROUP BY s
            """)
    Page<Song> findSongsByFilter(Pageable pageable, @Param("search") String search, @Param("genreId") Long genreId);

    @Query(
            value = """
                SELECT s.*
                FROM songs s
                INNER JOIN artist_songs ars ON ars.song_id = s.id
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE ars.artist_id = :artistId
                  AND (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                GROUP BY s.id
                ORDER BY COALESCE(SUM(usc.count_listen), 0) ASC
               """,
            countQuery = """
                SELECT COUNT(DISTINCT s.id)
                FROM songs s
                INNER JOIN artist_songs ars ON ars.song_id = s.id
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                WHERE ars.artist_id = :artistId
                  AND (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                """,
            nativeQuery = true
    )
    Page<Song> findArtistSongsOrderByListenCountAsc(
            Pageable pageable,
            @Param("search") String search,
            @Param("genreId") Long genreId,
            @Param("artistId") Long artistId
    );

    @Query(
            value = """
                SELECT s.*
                FROM songs s
                INNER JOIN artist_songs ars ON ars.song_id = s.id
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE ars.artist_id = :artistId
                  AND (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                GROUP BY s.id
                ORDER BY COALESCE(SUM(usc.count_listen), 0) DESC
               """,
            countQuery = """
                SELECT COUNT(DISTINCT s.id)
                FROM songs s
                INNER JOIN artist_songs ars ON ars.song_id = s.id
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                WHERE ars.artist_id = :artistId
                  AND (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                """,
            nativeQuery = true
    )
    Page<Song> findArtistSongsOrderByListenCountDesc(
            Pageable pageable,
            @Param("search") String search,
            @Param("genreId") Long genreId,
            @Param("artistId") Long artistId
    );

    @Query(
            value = """
                SELECT s.*
                FROM songs s
                INNER JOIN artist_songs ars ON ars.song_id = s.id
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE ars.artist_id = :artistId
                  AND (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                GROUP BY s.id
                ORDER BY COUNT(DISTINCT usc.user_id) ASC
               """,
            countQuery = """
                SELECT COUNT(DISTINCT s.id)
                FROM songs s
                INNER JOIN artist_songs ars ON ars.song_id = s.id
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                WHERE ars.artist_id = :artistId
                  AND (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                """,
            nativeQuery = true
    )
    Page<Song> findArtistSongsOrderByNumberOfListenersAsc(
            Pageable pageable,
            @Param("search") String search,
            @Param("genreId") Long genreId,
            @Param("artistId") Long artistId
    );

    @Query(
            value = """
                SELECT s.*
                FROM songs s
                INNER JOIN artist_songs ars ON ars.song_id = s.id
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE ars.artist_id = :artistId
                  AND (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                GROUP BY s.id
                ORDER BY COUNT(DISTINCT usc.user_id) DESC
               """,
            countQuery = """
                SELECT COUNT(DISTINCT s.id)
                FROM songs s
                INNER JOIN artist_songs ars ON ars.song_id = s.id
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                WHERE ars.artist_id = :artistId
                  AND (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                """,
            nativeQuery = true
    )
    Page<Song> findArtistSongsOrderByNumberOfListenersDesc(
            Pageable pageable,
            @Param("search") String search,
            @Param("genreId") Long genreId,
            @Param("artistId") Long artistId
    );

    @Query(
            value = """
                SELECT s.*
                FROM songs s
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                GROUP BY s.id
                ORDER BY COALESCE(SUM(usc.count_listen), 0) ASC
               """,
            countQuery = """
                SELECT COUNT(DISTINCT s.id)
                FROM songs s
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                """,
            nativeQuery = true
    )
    Page<Song> findSongsOrderByListenCountAsc(
            Pageable pageable,
            @Param("search") String search,
            @Param("genreId") Long genreId
    );

    @Query(
            value = """
                SELECT s.*
                FROM songs s
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                GROUP BY s.id
                ORDER BY COALESCE(SUM(usc.count_listen), 0) DESC
               """,
            countQuery = """
                SELECT COUNT(DISTINCT s.id)
                FROM songs s
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                """,
            nativeQuery = true
    )
    Page<Song> findSongsOrderByListenCountDesc(
            Pageable pageable,
            @Param("search") String search,
            @Param("genreId") Long genreId
    );

    @Query(
            value = """
                SELECT s.*
                FROM songs s
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                GROUP BY s.id
                ORDER BY COUNT(DISTINCT usc.user_id) ASC
               """,
            countQuery = """
                SELECT COUNT(DISTINCT s.id)
                FROM songs s
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                """,
            nativeQuery = true
    )
    Page<Song> findSongsOrderByNumberOfListenersAsc(
            Pageable pageable,
            @Param("search") String search,
            @Param("genreId") Long genreId
    );

    @Query(
            value = """
                SELECT s.*
                FROM songs s
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                GROUP BY s.id
                ORDER BY COUNT(DISTINCT usc.user_id) DESC
               """,
            countQuery = """
                SELECT COUNT(DISTINCT s.id)
                FROM songs s
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE (:search IS NULL OR LOWER(s.title) LIKE CONCAT('%', :search, '%'))
                  AND (:genreId IS NULL OR gs.genre_id = :genreId)
                """,
            nativeQuery = true
    )
    Page<Song> findSongsOrderByNumberOfListenersDesc(
            Pageable pageable,
            @Param("search") String search,
            @Param("genreId") Long genreId
    );

    @Query(nativeQuery = true, value = """
                SELECT s.*
                FROM songs s
                LEFT JOIN genre_songs gs ON gs.song_id = s.id
                LEFT JOIN user_song_count usc ON usc.song_id = s.id
                WHERE (:title IS NULL OR LOWER(s.title) LIKE CONCAT('%', :title, '%'))
                    AND (:genreId IS NULL OR gs.genre_id = :genreId)
                    AND (s.status LIKE 'ACCEPTED')
                GROUP BY s.id
                ORDER BY s.release_date DESC
                LIMIT :limit OFFSET :offset
            \s""")
    List<Song> getAllSongsByTitleOrGenreSongs(
            @Param("title") String title,
            @Param("genreId") Long genreId,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );
}
