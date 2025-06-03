package com.spring.repository;

import com.spring.entities.NotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationTokenRepository extends JpaRepository<NotificationToken, Long> {
    @Query("SELECT untd FROM NotificationToken untd WHERE untd.user.id = :userId")
    NotificationToken findByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
