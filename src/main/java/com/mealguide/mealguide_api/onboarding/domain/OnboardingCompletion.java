package com.mealguide.mealguide_api.onboarding.domain;

import java.util.List;

public record OnboardingCompletion(
        String languageCode,
        Long schoolId,
        List<String> allergyCodes,
        String religiousCode,
        boolean onboardingCompleted
) {
}
