package com.mealguide.mealguide_api.mealcrawl.application.dto;

public record RestrictionIngredientRow(
        String restrictionCode,
        String ingredientCode,
        String ingredientName
) {
}
