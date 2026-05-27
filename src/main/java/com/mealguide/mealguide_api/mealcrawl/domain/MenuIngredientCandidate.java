package com.mealguide.mealguide_api.mealcrawl.domain;

import java.math.BigDecimal;

public record MenuIngredientCandidate(
        String ingredientCode,
        String ingredientName,
        BigDecimal confidence
) {
    public MenuIngredientCandidate(String ingredientCode, BigDecimal confidence) {
        this(ingredientCode, null, confidence);
    }
}

