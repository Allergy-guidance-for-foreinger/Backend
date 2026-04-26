package com.mealguide.mealguide_api.mealcrawl.application.dto;

public record MealMenuIngredientRow(
        Long mealMenuId,
        String ingredientCode,
        String ingredientName
) {
}
