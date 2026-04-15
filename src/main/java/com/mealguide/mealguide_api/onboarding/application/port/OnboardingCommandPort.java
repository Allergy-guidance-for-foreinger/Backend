package com.mealguide.mealguide_api.onboarding.application.port;

import com.mealguide.mealguide_api.onboarding.domain.OnboardingUser;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OnboardingCommandPort {
    Optional<OnboardingUser> findActiveUserById(Long userId);

    boolean existsSchoolById(Long schoolId);

    boolean existsLanguageCode(String languageCode);

    boolean existsAllAllergyCodes(Set<String> allergyCodes);

    boolean existsReligiousCode(String religiousCode);

    void replaceAllergies(Long userId, List<String> allergyCodes);
}
