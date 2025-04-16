package com.spring.repository;

import com.spring.constants.UserType;
import com.spring.entities.Artist;
import com.spring.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
                SELECT u FROM Artist u
                WHERE u.userType = :userType
                AND (:status IS NULL OR u.status = :status)
                AND (
                    LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                    LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                    LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                )
            """)
    Page<Artist> searchByUserTypeAndStatusAndKeyword(
            @Param("userType") UserType userType,
            @Param("status") Integer status,
            @Param("search") String search,
            Pageable pageable
    );
}
