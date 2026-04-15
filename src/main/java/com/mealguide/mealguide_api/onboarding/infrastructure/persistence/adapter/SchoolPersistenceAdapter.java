package com.mealguide.mealguide_api.onboarding.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.onboarding.application.port.OnboardingCommandPort;
import com.mealguide.mealguide_api.onboarding.application.port.SchoolQueryPort;
import com.mealguide.mealguide_api.onboarding.domain.OnboardingUser;
import com.mealguide.mealguide_api.onboarding.domain.OnboardingUserAllergy;
import com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository.OnboardingUserAllergyJpaRepository;
import com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository.OnboardingUserJpaRepository;
import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;
import com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository.SchoolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SchoolPersistenceAdapter implements SchoolQueryPort, OnboardingCommandPort {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final SchoolJpaRepository schoolJpaRepository;
    private final OnboardingUserJpaRepository onboardingUserJpaRepository;
    private final OnboardingUserAllergyJpaRepository onboardingUserAllergyJpaRepository;

    @Override
    public List<SchoolOption> findSchools(String langCode) {
        return schoolJpaRepository.findSchoolOptions(langCode);
    }

    @Override
    public Optional<OnboardingUser> findActiveUserById(Long userId) {
        return onboardingUserJpaRepository.findByIdAndDeletedAtIsNullAndStatus(userId, ACTIVE_STATUS);
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
}
