package com.spring.repository;

import com.spring.entities.UserSongCount;
import com.spring.entities.UserSongCountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSongCountRepository extends JpaRepository<UserSongCount, UserSongCountId> {
    @Query("""
                SELECT COALESCE(SUM(userSongCount.countListen), 0)
                FROM UserSongCount userSongCount
                WHERE userSongCount.userSongCountId.song.id = :songId
            """)
    Long getTotalCountListenBySongId(@Param("songId") Long songId);

    @Query(value = """
                SELECT song_id, SUM(count_listen) AS total_listen
                FROM user_song_count
                GROUP BY song_id
                ORDER BY total_listen DESC
                LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findTopSongsByListenCount(
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    @Query("SELECT COUNT(DISTINCT usc.userSongCountId.user.id) FROM UserSongCount usc WHERE usc.userSongCountId.song.id = :songId AND usc.userSongCountId.user.userType = 'USER'")
    Long countDistinctUsersBySongId(@Param("songId") Long songId);

    @Query("SELECT COUNT(DISTINCT user.id) " +
           "FROM UserSongCount usc " +
           "JOIN usc.userSongCountId.user user " +
           "JOIN usc.userSongCountId.song song " +
           "WHERE song.id IN :songIds")
    Long countDistinctListenersBySongIds(@Param("songIds") List<Long> songIds);
}

