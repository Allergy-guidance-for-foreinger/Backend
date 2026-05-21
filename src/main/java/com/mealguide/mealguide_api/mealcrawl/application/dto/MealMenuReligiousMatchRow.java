package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.math.BigDecimal;

public record MealMenuReligiousMatchRow(
        Long mealMenuId,
        String ingredientCode,
        String ingredientName,
        BigDecimal confidence,
        String restrictionCode,
        String restrictionName
) {
}
