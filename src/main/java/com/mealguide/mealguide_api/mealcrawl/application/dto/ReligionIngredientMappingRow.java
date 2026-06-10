package com.mealguide.mealguide_api.mealcrawl.application.dto;

public record ReligionIngredientMappingRow(
        String ingredientCode,
        String restrictionCode,
        String koreanName,
        String englishName
) {
}
