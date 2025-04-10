package com.spring.repository;

import com.spring.entities.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    @Query("SELECT COUNT(a) FROM Artist a")
    Long countAllArtists();
}
