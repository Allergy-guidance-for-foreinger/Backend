package com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.onboarding.domain.OnboardingUserAllergy;
import com.mealguide.mealguide_api.onboarding.domain.OnboardingUserAllergyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OnboardingUserAllergyJpaRepository extends JpaRepository<OnboardingUserAllergy, OnboardingUserAllergyId> {

    @Modifying
    @Query("delete from OnboardingUserAllergy ua where ua.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}

