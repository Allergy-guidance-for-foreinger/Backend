package com.mealguide.mealguide_api.onboarding.application.port;

import java.util.List;
import java.util.Set;

public interface OnboardingCommandPort {
    boolean existsActiveUserById(Long userId);

    boolean existsSchoolById(Long schoolId);

    boolean existsLanguageCode(String languageCode);

    boolean existsAllAllergyCodes(Set<String> allergyCodes);

    boolean existsReligiousCode(String religiousCode);

    void replaceAllergies(Long userId, List<String> allergyCodes);

    boolean completeOnboarding(Long userId, String languageCode, Long schoolId, String religiousCode);
}

