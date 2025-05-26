package com.spring.repository;

import com.spring.entities.UserNotificationsTokenDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationTokenDeviceRepository extends JpaRepository<UserNotificationsTokenDevice, Long> {
    @Query("SELECT untd FROM UserNotificationsTokenDevice untd WHERE untd.user.id = :userId")
    UserNotificationsTokenDevice findByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
