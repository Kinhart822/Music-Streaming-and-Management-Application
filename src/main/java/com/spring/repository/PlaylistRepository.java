package com.spring.repository;

import com.spring.entities.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    @Query("SELECT asg.artistPlaylistId.playlist FROM ArtistPlaylist asg WHERE asg.artistPlaylistId.artist.id = :artistId")
    List<Playlist> findByArtistId(@Param("artistId") Long artistId);

    @Query("SELECT COUNT(a) FROM Playlist a")
    Long countAllPlaylists();

    @Query("SELECT COUNT(a) FROM Playlist a WHERE a.playlistAndAlbumStatus = 'PENDING'")
    Long countAllPendingPlaylists();

    @Query("""
            SELECT p FROM Playlist p
            LEFT JOIN ArtistPlaylist api ON api.artistPlaylistId.playlist = p
            WHERE (:search IS NULL OR LOWER(p.playlistName) LIKE %:search%)
              AND (api.artistPlaylistId.artist.id = :artistId)
            GROUP BY p
            """)
    Page<Playlist> findArtistPlaylistsByFilter(Pageable pageable, @Param("search") String search, @Param("artistId") Long artistId);

    @Query("""
            SELECT p FROM Playlist p
            LEFT JOIN ArtistPlaylist api ON api.artistPlaylistId.playlist = p
            WHERE (:search IS NULL OR LOWER(p.playlistName) LIKE %:search%)
            GROUP BY p
            """)
    Page<Playlist> findPlaylistsByFilter(Pageable pageable, @Param("search") String search);

    @Query(nativeQuery = true, value = """
                SELECT p.*
                FROM playlists p
                LEFT JOIN artist_playlists aps ON aps.playlist_id = p.id
                WHERE (:title IS NULL OR LOWER(p.playlist_name) LIKE CONCAT('%', :title, '%'))
                    AND (p.status LIKE 'ACCEPTED')
                GROUP BY p.id
                ORDER BY p.release_date DESC
                LIMIT :limit OFFSET :offset
            \s""")
    List<Playlist> getAllPlaylistsByTitle(
            @Param("title") String title,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );
}
