package com.spring.repository;

import com.spring.entities.Artist;
import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    @Query("SELECT COUNT(a) FROM Artist a")
    Long countAllArtists();

    List<Artist> findAllByStatus(int status);

    @Query("SELECT auf.artistUserFollowId.artist FROM ArtistUserFollow auf WHERE auf.artistUserFollowId.user = :user")
    List<Artist> findByUser(@Param("user") User user);

    @Query(value = """
                 SELECT a.*, u.*\s
                 FROM artists a
                 JOIN users u ON a.id = u.id
                 WHERE (:title IS NULL OR LOWER(a.artist_name) LIKE %:title%)
                 ORDER BY u.created_date DESC
                 LIMIT :limit OFFSET :offset
            \s""", nativeQuery = true)
    List<Artist> getAllArtistsByTitle(
            @Param("title") String title,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );
}
