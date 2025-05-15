package com.spring.repository;

import com.spring.constants.UserType;
import com.spring.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<User> findAllByStatus(Integer status);

    @Query("SELECT COUNT(a) FROM User a WHERE LOWER(a.userType) = 'user'")
    Long countAllUsers();

    List<User> findByUserType(UserType userType);

    @Query("""
                SELECT u FROM User u
                WHERE u.userType = :userType
                AND (:status IS NULL OR u.status = :status)
                AND (
                    LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                    LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                    LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                )
            """)
    Page<User> searchByUserTypeAndStatusAndKeyword(
            @Param("userType") UserType userType,
            @Param("status") Integer status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
                SELECT u FROM User u
                WHERE (
                    LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                    LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                    LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                )
            """)
    Page<User> searchByKeyword(@Param("search") String search, Pageable pageable);

    @Query("""
    SELECT u FROM User u
    WHERE u.userType = :userType
    AND (
        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
        LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
        LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
    )
""")
    Page<User> searchByUserTypeAndKeyword(@Param("userType") UserType userType, @Param("search") String search, Pageable pageable);

    @Query("""
    SELECT u FROM User u
    WHERE u.status = :status
    AND (
        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
        LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
        LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
    )
""")
    Page<User> searchByStatusAndKeyword(@Param("status") Integer status, @Param("search") String search, Pageable pageable);

}
