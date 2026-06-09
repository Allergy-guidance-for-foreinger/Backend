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
        if (allergiesByMealMenuId == null || allergiesByMealMenuId.isEmpty()) {
            allergiesByMealMenuId = Map.of();
        } else {
            Map<Long, List<AllergyData>> copied = new java.util.LinkedHashMap<>();
            for (Map.Entry<Long, List<AllergyData>> entry : allergiesByMealMenuId.entrySet()) {
                copied.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
            }
            allergiesByMealMenuId = Map.copyOf(copied);
        }
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
