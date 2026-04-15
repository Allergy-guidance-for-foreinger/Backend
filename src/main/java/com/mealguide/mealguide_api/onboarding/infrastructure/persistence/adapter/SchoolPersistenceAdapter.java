package com.mealguide.mealguide_api.onboarding.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.onboarding.application.port.OnboardingCommandPort;
import com.mealguide.mealguide_api.onboarding.application.port.SchoolQueryPort;
import com.mealguide.mealguide_api.onboarding.domain.OnboardingUserAllergy;
import com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository.OnboardingUserAllergyJpaRepository;
import com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository.OnboardingUserJpaRepository;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;
import com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository.SchoolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SchoolPersistenceAdapter implements SchoolQueryPort, OnboardingCommandPort {

    private static final UserStatus ACTIVE_STATUS = UserStatus.ACTIVE;

    private final SchoolJpaRepository schoolJpaRepository;
    private final OnboardingUserJpaRepository onboardingUserJpaRepository;
    private final OnboardingUserAllergyJpaRepository onboardingUserAllergyJpaRepository;

    @Override
    public List<SchoolOption> findSchools(String langCode) {
        return schoolJpaRepository.findSchoolOptions(langCode);
    }

    @Override
    public boolean existsActiveUserById(Long userId) {
        return onboardingUserJpaRepository.existsByIdAndDeletedAtIsNullAndStatus(userId, ACTIVE_STATUS);
    }

    @Override
    public boolean existsSchoolById(Long schoolId) {
        return schoolJpaRepository.existsById(schoolId);
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
    public boolean existsReligiousCode(String religiousCode) {
        return onboardingUserJpaRepository.existsReligiousCode(religiousCode);
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
    public boolean completeOnboarding(Long userId, String languageCode, Long schoolId, String religiousCode) {
        return onboardingUserJpaRepository.completeOnboarding(userId, languageCode, schoolId, religiousCode, ACTIVE_STATUS) > 0;
    }
}
