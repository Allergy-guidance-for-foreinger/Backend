package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record WeeklyMealRiskDataCachePayload(
        Map<Long, IngredientData> ingredientsByMealMenuId,
        Map<Long, List<AllergyData>> allergiesByMealMenuId
) {
    public WeeklyMealRiskDataCachePayload {
        if (ingredientsByMealMenuId == null || ingredientsByMealMenuId.isEmpty()) {
            ingredientsByMealMenuId = Map.of();
        } else {
            Map<Long, IngredientData> copiedIngredients = new java.util.LinkedHashMap<>();
            ingredientsByMealMenuId.forEach((mealMenuId, ingredientData) -> {
                if (mealMenuId != null && ingredientData != null) {
                    copiedIngredients.put(mealMenuId, ingredientData);
                }
            });
            ingredientsByMealMenuId = Map.copyOf(copiedIngredients);
        }
        if (allergiesByMealMenuId == null || allergiesByMealMenuId.isEmpty()) {
            allergiesByMealMenuId = Map.of();
        } else {
            Map<Long, List<AllergyData>> copied = new java.util.LinkedHashMap<>();
            for (Map.Entry<Long, List<AllergyData>> entry : allergiesByMealMenuId.entrySet()) {
                if (entry.getKey() != null) {
                    copied.put(entry.getKey(), entry.getValue() == null ? List.of() : entry.getValue().stream()
                            .filter(Objects::nonNull)
                            .toList());
                }
            }
            allergiesByMealMenuId = Map.copyOf(copied);
        }
    }

    public record IngredientData(
            String source,
            List<String> ingredientCodes
    ) {
        public IngredientData {
            ingredientCodes = ingredientCodes == null ? List.of() : ingredientCodes.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    public record AllergyData(
            String allergyCode,
            BigDecimal confidence
    ) {
    }
}
