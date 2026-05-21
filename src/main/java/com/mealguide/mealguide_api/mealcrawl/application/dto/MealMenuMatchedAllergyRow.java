package com.mealguide.mealguide_api.mealcrawl.application.dto;

public record MealMenuMatchedAllergyRow(
        Long mealMenuId,
        String allergyCode,
        String allergyName,
        String reason
) {
}

