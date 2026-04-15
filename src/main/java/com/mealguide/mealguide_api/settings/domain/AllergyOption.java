package com.mealguide.mealguide_api.settings.domain;

public record AllergyOption(
        String code,
        String name,
        int displayOrder
) {
}
