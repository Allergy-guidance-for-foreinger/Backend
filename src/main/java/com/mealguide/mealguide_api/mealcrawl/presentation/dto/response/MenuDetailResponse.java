package com.mealguide.mealguide_api.mealcrawl.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MenuDetailResponse(
        Long mealMenuId,
        String menuName,
        String description,
        String cornerName,
        Integer displayOrder,
        Long spicyLevel,
        boolean aiAnalyzed,
        List<AllergyResponse> allergies,
        List<MatchedAllergyResponse> matchedAllergies,
        List<IngredientResponse> ingredients,
        List<MatchedReligiousIngredientResponse> matchedReligiousIngredients,
        LikeResponse like,
        ReviewSummaryResponse review
) {
    public record AllergyResponse(
            String code,
            String name,
            String source
    ) {
    }

    public record IngredientResponse(
            String code,
            String name,
            String source
    ) {
    }

    public record MatchedAllergyResponse(
            String code,
            String name,
            String riskLevel,
            BigDecimal confidence
    ) {
    }

    public record MatchedReligiousIngredientResponse(
            String ingredientCode,
            String ingredientName,
            BigDecimal confidence,
            List<MatchedReligiousRestrictionResponse> matchedReligiousRestrictions
    ) {
    }

    public record MatchedReligiousRestrictionResponse(
            String religiousRestrictionCode,
            String religiousRestrictionName,
            String riskLevel
    ) {
    }

    public record LikeResponse(
            long count,
            boolean likedByMe
    ) {
    }

    public record ReviewSummaryResponse(
            long count
    ) {
    }
}
