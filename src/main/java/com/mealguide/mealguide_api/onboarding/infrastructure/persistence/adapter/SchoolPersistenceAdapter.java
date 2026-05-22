package com.mealguide.mealguide_api.onboarding.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.onboarding.application.port.OnboardingCommandPort;
import com.mealguide.mealguide_api.onboarding.domain.OnboardingUserAllergy;
import com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository.OnboardingUserAllergyJpaRepository;
import com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository.OnboardingUserJpaRepository;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SchoolPersistenceAdapter implements OnboardingCommandPort {

    private static final UserStatus ACTIVE_STATUS = UserStatus.ACTIVE;

    private final OnboardingUserJpaRepository onboardingUserJpaRepository;
    private final OnboardingUserAllergyJpaRepository onboardingUserAllergyJpaRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public boolean existsActiveUserById(Long userId) {
        return onboardingUserJpaRepository.existsByIdAndDeletedAtIsNullAndStatus(userId, ACTIVE_STATUS);
    }

    @Override
    public boolean existsSchoolById(Long schoolId) {
        return onboardingUserJpaRepository.existsSchoolById(schoolId);
    }

    @Override
    public boolean existsLanguageCode(String languageCode) {
        return onboardingUserJpaRepository.existsLanguageCode(languageCode);
    }

    @Override
    public boolean existsAllAllergyCodes(Set<String> allergyCodes) {
        if (allergyCodes.isEmpty()) {
            return true;
        }
        return onboardingUserJpaRepository.countAllergyCodes(allergyCodes) == allergyCodes.size();
    }

    @Override
    public boolean existsAllReligiousCodes(Set<String> religiousCodes) {
        if (religiousCodes.isEmpty()) {
            return true;
        }
        return onboardingUserJpaRepository.countReligiousCodes(religiousCodes) == religiousCodes.size();
    }

    @Override
    public boolean existsCountryCode(String countryCode) {
        return onboardingUserJpaRepository.existsCountryCode(countryCode);
    }

    @Override
    public void replaceAllergies(Long userId, List<String> allergyCodes) {
        onboardingUserAllergyJpaRepository.deleteByUserId(userId);
        List<OnboardingUserAllergy> userAllergies = allergyCodes.stream()
                .map(allergyCode -> OnboardingUserAllergy.create(userId, allergyCode))
                .toList();
        onboardingUserAllergyJpaRepository.saveAll(userAllergies);
    }

    @Override
    public void replaceReligiousRestrictions(Long userId, List<String> religiousCodes) {
        String deleteSql = """
                delete from user_religious_food_restriction
                where user_id = :userId
                """;
        namedParameterJdbcTemplate.update(deleteSql, new MapSqlParameterSource("userId", userId));
        if (religiousCodes.isEmpty()) {
            return;
        }

        String insertSql = """
                insert into user_religious_food_restriction (user_id, religious_food_restriction_code, created_at)
                values (:userId, :religiousCode, now())
                """;
        for (String religiousCode : religiousCodes) {
            namedParameterJdbcTemplate.update(insertSql, new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("religiousCode", religiousCode));
        }
    }

    @Override
    public boolean completeOnboarding(Long userId, String languageCode, Long schoolId, String countryCode) {
        return onboardingUserJpaRepository.completeOnboarding(
                userId,
                languageCode,
                schoolId,
                countryCode,
                ACTIVE_STATUS
        ) > 0;
    }
}

