package com.spring.repository;

import com.spring.entities.HistoryListen;
import com.spring.entities.Song;
import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryListenRepository extends JpaRepository<HistoryListen,Long> {
    List<HistoryListen> findAllByUserAndSong(User user, Song song);
}
