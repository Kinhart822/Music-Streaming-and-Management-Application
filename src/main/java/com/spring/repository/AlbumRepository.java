package com.spring.repository;

import com.spring.entities.Album;
import com.spring.entities.Playlist;
import com.spring.entities.User;
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

    @Query("SELECT auf.userSavedAlbumId.album FROM UserSavedAlbum auf WHERE auf.userSavedAlbumId.user = :user")
    List<Album> findByUser(@Param("user") User user);

    @Query("SELECT COUNT(a) FROM Album a")
    Long countAllAlbums();

    @Query("SELECT COUNT(a) FROM Album a WHERE a.playlistAndAlbumStatus = 'PENDING'")
    Long countAllPendingAlbums();

    @Query("""
            SELECT a FROM Album a
            LEFT JOIN ArtistAlbum aai ON aai.artistAlbumId.album = a
            WHERE (:search IS NULL OR LOWER(a.albumName) LIKE %:search%)
              AND (aai.artistAlbumId.artist.id = :artistId)
            GROUP BY a
            """)
    Page<Album> findArtistAlbumsByFilter(Pageable pageable, @Param("search") String search, @Param("artistId") Long artistId);

    @Query("""
            SELECT a FROM Album a
            LEFT JOIN ArtistAlbum aai ON aai.artistAlbumId.album = a
            WHERE (:search IS NULL OR LOWER(a.albumName) LIKE %:search%)
            GROUP BY a
            """)
    Page<Album> findAlbumsByFilter(Pageable pageable, @Param("search") String search);

    @Query(nativeQuery = true, value = """
                SELECT a.*
                FROM albums a
                LEFT JOIN artist_albums abs ON abs.album_id = a.id
                WHERE (:title IS NULL OR LOWER(a.album_name) LIKE CONCAT('%', :title, '%'))
                    AND (a.status LIKE 'ACCEPTED')
                GROUP BY a.id
                ORDER BY a.release_date DESC
                LIMIT :limit OFFSET :offset
            """)
    List<Album> getAllAlbumsByTitle(
            @Param("title") String title,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );
}
