package com.mealguide.mealguide_api.mealcrawl.domain;

import java.math.BigDecimal;

public record MenuIngredientCandidate(
        String ingredientCode,
        BigDecimal confidence
) {
}

