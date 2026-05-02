package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceJpaRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByIdAndDeletedAtIsNullAndStatus(Long userId, String status);
}

