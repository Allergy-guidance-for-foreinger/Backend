package com.mealguide.mealguide_api.onboarding.domain;

import java.util.List;

public record OnboardingCompletion(
        String languageCode,
        Long schoolId,
        List<String> allergyCodes,
        List<String> religiousCodes,
        String countryCode,
        boolean onboardingCompleted
) {
}

