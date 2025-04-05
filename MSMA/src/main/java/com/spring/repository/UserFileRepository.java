package com.spring.repository;

import com.spring.entities.User;
import com.spring.entities.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFileRepository extends JpaRepository<UserFile, Long> {
    void deleteAllByUser(User user);
}
