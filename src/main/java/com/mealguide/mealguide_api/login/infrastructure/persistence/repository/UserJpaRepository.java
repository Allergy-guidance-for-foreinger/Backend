package com.mealguide.mealguide_api.login.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserRole;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdAndDeletedAtIsNullAndStatus(Long userId, UserStatus status);

    @Query("""
            select u.role
            from User u
            where u.id = :userId
              and u.deletedAt is null
              and u.status = :status
            """)
    Optional<UserRole> findRoleByIdAndDeletedAtIsNullAndStatus(
            @Param("userId") Long userId,
            @Param("status") UserStatus status
    );

    boolean existsByIdAndDeletedAtIsNullAndStatus(Long userId, UserStatus status);
}
