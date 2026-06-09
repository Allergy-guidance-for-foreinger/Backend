package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record MenuDetailBaseCachePayload(
        Long mealMenuId,
        Long cafeteriaId,
        Long menuId,
        Long schoolId,
        String menuName,
        String description,
        String cornerName,
        Integer displayOrder,
        Long spicyLevel,
        boolean aiAnalyzed,
        List<IngredientData> ingredients,
        List<AllergyData> allergies
) {
    public MenuDetailBaseCachePayload {
        ingredients = ingredients == null ? List.of() : ingredients.stream()
                .filter(Objects::nonNull)
                .toList();
        allergies = allergies == null ? List.of() : allergies.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    public record IngredientData(
            String code,
            String name,
            String source
    ) {
    }

    public record AllergyData(
            String code,
            String name,
            BigDecimal confidence
    ) {
    }
}
