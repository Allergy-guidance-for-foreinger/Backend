package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.util.List;

public record CurrentUserMealPreference(
        Long userId,
        Long schoolId,
        String languageCode,
        List<String> religiousCodes,
        List<String> allergyCodes
) {
}
