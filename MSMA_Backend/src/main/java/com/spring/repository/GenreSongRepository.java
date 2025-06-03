package com.spring.repository;

import com.spring.entities.GenreSong;
import com.spring.entities.GenreSongId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreSongRepository extends JpaRepository<GenreSong, GenreSongId> {
    @Query("SELECT gs FROM GenreSong gs WHERE gs.genreSongId.genre.id = :genreId")
    List<GenreSong> findAllByGenreId(@Param("genreId") Long genreId);
}
