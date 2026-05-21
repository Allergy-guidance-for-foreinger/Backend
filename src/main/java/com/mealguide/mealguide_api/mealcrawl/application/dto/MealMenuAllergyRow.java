package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.math.BigDecimal;

public record MealMenuAllergyRow(
        Long mealMenuId,
        String allergyCode,
        String allergyName,
        BigDecimal confidence
) {
}
