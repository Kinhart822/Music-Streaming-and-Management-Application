package com.spring.repository;

import com.spring.entities.Playlist;
import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayListRepository extends JpaRepository<Playlist, Long> {
    void deleteAllByUser(User user);
    List <Playlist> findByUserId(Long userId);
}
