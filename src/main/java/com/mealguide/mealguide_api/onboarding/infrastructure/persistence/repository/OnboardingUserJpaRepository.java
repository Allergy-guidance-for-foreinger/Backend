package com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.onboarding.domain.OnboardingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface OnboardingUserJpaRepository extends JpaRepository<OnboardingUser, Long> {
    Optional<OnboardingUser> findByIdAndDeletedAtIsNullAndStatus(Long userId, String status);

    @Query(value = """
            select count(a.code)
            from allergy a
            where a.code in (:codes)
            """, nativeQuery = true)
    long countAllergyCodes(@Param("codes") Set<String> codes);

    @Query(value = """
            select exists(
                select 1
                from language l
                where l.code = :languageCode
            )
            """, nativeQuery = true)
    boolean existsLanguageCode(@Param("languageCode") String languageCode);

    @Query(value = """
            select exists(
                select 1
                from religious_food_restriction r
                where r.code = :religiousCode
            )
            """, nativeQuery = true)
    boolean existsReligiousCode(@Param("religiousCode") String religiousCode);
}
