package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record MenuDetailRiskDataCachePayload(
        String ingredientSource,
        List<IngredientData> ingredients,
        List<AllergyData> allergies
) {
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
