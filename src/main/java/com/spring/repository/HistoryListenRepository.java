package com.spring.repository;

import com.spring.entities.HistoryListen;
import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryListenRepository extends JpaRepository<HistoryListen,Long> {
    void deleteAllByUser(User user);

    @Modifying
    @Query("DELETE FROM HistoryListen ps WHERE ps.song.id = :songId")
    void deleteAllBySongId(@Param("songId") Long songId);
}
