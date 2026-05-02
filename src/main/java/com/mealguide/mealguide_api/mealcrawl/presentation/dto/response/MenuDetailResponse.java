package com.mealguide.mealguide_api.mealcrawl.presentation.dto.response;

import java.util.List;

public record MenuDetailResponse(
        Long mealMenuId,
        String menuName,
        String description,
        String cornerName,
        Integer displayOrder,
        Long spicyLevel,
        boolean aiAnalyzed,
        MenuRiskResponse risk,
        List<IngredientResponse> ingredients,
        List<MatchedAllergyResponse> matchedAllergies
) {
    public record MenuRiskResponse(
            String riskLevel
    ) {
    }

    public record IngredientResponse(
            String code,
            String name,
            String source
    ) {
    }

    public record MatchedAllergyResponse(
            String allergyCode,
            String allergyName,
            String ingredientCode,
            String ingredientName,
            String message
    ) {
    }
}
