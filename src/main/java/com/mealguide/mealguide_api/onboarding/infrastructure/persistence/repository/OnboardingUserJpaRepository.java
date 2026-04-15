package com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface OnboardingUserJpaRepository extends JpaRepository<User, Long> {
    boolean existsByIdAndDeletedAtIsNullAndStatus(Long userId, UserStatus status);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User u
            set u.languageCode = :languageCode,
                u.schoolId = :schoolId,
                u.religiousCode = :religiousCode,
                u.onboardingCompleted = true
            where u.id = :userId
              and u.deletedAt is null
              and u.status = :status
            """)
    int completeOnboarding(
            @Param("userId") Long userId,
            @Param("languageCode") String languageCode,
            @Param("schoolId") Long schoolId,
            @Param("religiousCode") String religiousCode,
            @Param("status") UserStatus status
    );
}
