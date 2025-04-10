package com.spring.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistUserFollowRepository {
    @Query("SELECT COUNT(u) FROM ArtistUserFollow u WHERE u.artistUserFollowId.artist.id = :artistId")
    Long countByArtistId(@Param("artistId") Long artistId);
}
