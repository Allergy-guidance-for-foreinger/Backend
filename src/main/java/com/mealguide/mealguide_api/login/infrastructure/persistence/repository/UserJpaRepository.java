package com.mealguide.mealguide_api.login.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdAndDeletedAtIsNullAndStatus(Long userId, UserStatus status);
}
