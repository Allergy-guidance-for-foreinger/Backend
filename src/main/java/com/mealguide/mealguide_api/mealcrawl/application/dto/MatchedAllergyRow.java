package com.mealguide.mealguide_api.mealcrawl.application.dto;

public record MatchedAllergyRow(
        String allergyCode,
        String allergyName,
        String ingredientCode,
        String ingredientName
) {
}
