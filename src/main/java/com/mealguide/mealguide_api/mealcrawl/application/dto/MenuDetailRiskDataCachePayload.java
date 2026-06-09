package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record MenuDetailRiskDataCachePayload(
        String ingredientSource,
        List<IngredientData> ingredients,
        List<AllergyData> allergies
) {
    public MenuDetailRiskDataCachePayload {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        allergies = allergies == null ? List.of() : List.copyOf(allergies);
    }

    public record IngredientData(
            String code,
            BigDecimal confidence
    ) {
    }

    public record AllergyData(
            String code,
            BigDecimal confidence
    ) {
    }
}
