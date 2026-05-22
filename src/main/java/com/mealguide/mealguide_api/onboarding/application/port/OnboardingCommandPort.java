package com.mealguide.mealguide_api.onboarding.application.port;

import java.util.List;
import java.util.Set;

public interface OnboardingCommandPort {
    boolean existsActiveUserById(Long userId);

    boolean existsSchoolById(Long schoolId);

    boolean existsLanguageCode(String languageCode);

    boolean existsAllAllergyCodes(Set<String> allergyCodes);

    boolean existsAllReligiousCodes(Set<String> religiousCodes);

    boolean existsCountryCode(String countryCode);

    void replaceAllergies(Long userId, List<String> allergyCodes);

    void replaceReligiousRestrictions(Long userId, List<String> religiousCodes);

    boolean completeOnboarding(Long userId, String languageCode, Long schoolId, String countryCode);
}

