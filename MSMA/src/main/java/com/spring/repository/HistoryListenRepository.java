package com.spring.repository;

import com.spring.entities.HistoryListen;
import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryListenRepository extends JpaRepository<HistoryListen,Long> {
    void deleteAllByUser(User user);
}
