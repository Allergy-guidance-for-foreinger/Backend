package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserPreferenceJpaRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByIdAndDeletedAtIsNullAndStatus(Long userId, String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserPreference u
            set u.countryCode = :countryCode
            where u.id = :userId
              and u.deletedAt is null
              and u.status = :status
            """)
    int updateCountryCode(
            @Param("userId") Long userId,
            @Param("countryCode") String countryCode,
            @Param("status") String status
    );
}

