package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.math.BigDecimal;

public record PythonMenuIngredientResultDto(
        String ingredientCode,
        BigDecimal confidence
) {
}



