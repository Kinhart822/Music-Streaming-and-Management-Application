package com.spring.repository;

import com.spring.entities.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    @Query("SELECT p FROM Playlist p WHERE LOWER(p.playlistName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Playlist> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);


}
