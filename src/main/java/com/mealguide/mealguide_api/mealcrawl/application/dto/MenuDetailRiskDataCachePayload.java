package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record MenuDetailRiskDataCachePayload(
        String ingredientSource,
        List<IngredientData> ingredients,
        List<AllergyData> allergies
) {
    public MenuDetailRiskDataCachePayload {
        ingredients = ingredients == null ? List.of() : ingredients.stream()
                .filter(Objects::nonNull)
                .toList();
        allergies = allergies == null ? List.of() : allergies.stream()
                .filter(Objects::nonNull)
                .toList();
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
