package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.math.BigDecimal;

public record MenuDetailIngredientRow(
        Long mealMenuId,
        String ingredientCode,
        String ingredientName,
        String source,
        BigDecimal confidence
) {
}
