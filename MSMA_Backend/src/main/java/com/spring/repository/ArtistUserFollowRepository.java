package com.spring.repository;

import com.spring.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistUserFollowRepository extends JpaRepository<ArtistUserFollow, ArtistUserFollowId> {
    @Query("SELECT asg.artistUserFollowId.user.id FROM ArtistUserFollow asg WHERE asg.artistUserFollowId.artist.id = :artistId")
    List<Long> findByArtistId(@Param("artistId") Long artistId);

    @Query("SELECT COUNT(DISTINCT usc.artistUserFollowId.user.id) FROM ArtistUserFollow usc WHERE usc.artistUserFollowId.artist.id = :artistId AND usc.artistUserFollowId.user.userType = 'USER'")
    Long countDistinctUsersByArtistId(@Param("artistId") Long artistId);

    @Query("""
                SELECT CASE WHEN COUNT(usc) > 0 THEN true ELSE false END
                FROM ArtistUserFollow usc
                WHERE usc.artistUserFollowId.artist.id = :artistId AND usc.artistUserFollowId.user.id = :userId
            """)
    boolean existsByUserIdAndArtistId(@Param("userId") Long userId, @Param("artistId") Long artistId);
}
