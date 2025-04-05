package com.spring.repository;

import com.spring.entities.RefreshToken;
import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    boolean existsByUserAndStatus(User user, Integer status);
    List<RefreshToken> findAllByUserAndStatus(User user, Integer status);
    void deleteAllByUser(User user);
}
