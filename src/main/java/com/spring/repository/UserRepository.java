package com.spring.repository;

import com.spring.constants.UserType;
import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCaseAndStatus(String email, Integer status);

    Optional<User> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByResetKeyAndStatusAndResetDueDateIsAfter(String resetKey, Integer userType, Instant time);

    boolean existsByResetKey(String resetKey);

    List<User> findAllByStatusAndLastModifiedDateBefore(Integer status, Instant time);

    @Query("SELECT COUNT(a) FROM User a WHERE LOWER(a.userType) = 'user'")
    Long countAllUsers();

    List<User> findByUserType(UserType userType);
}
