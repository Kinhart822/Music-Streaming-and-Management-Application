package com.spring.repository;

import com.spring.entities.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    List<Genre> findByGenresNameIn(List<String> genresName);

    @Query("SELECT g FROM Genre g WHERE LOWER(g.genresName) = LOWER(:name)")
    Optional<Genre> findByGenresNameIgnoreCase(@Param("name") String name);

}
