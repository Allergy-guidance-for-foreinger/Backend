package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record WeeklyMealRiskDataCachePayload(
        Map<Long, IngredientData> ingredientsByMealMenuId,
        Map<Long, List<AllergyData>> allergiesByMealMenuId
) {
    public WeeklyMealRiskDataCachePayload {
        ingredientsByMealMenuId = ingredientsByMealMenuId == null ? Map.of() : Map.copyOf(ingredientsByMealMenuId);
        allergiesByMealMenuId = allergiesByMealMenuId == null ? Map.of() : Map.copyOf(allergiesByMealMenuId);
    }

    public record IngredientData(
            String source,
            List<String> ingredientCodes
    ) {
        public IngredientData {
            ingredientCodes = ingredientCodes == null ? List.of() : List.copyOf(ingredientCodes);
        }
    }

    public record AllergyData(
            String allergyCode,
            BigDecimal confidence
    ) {
    }
}
