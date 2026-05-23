package com.mealguide.mealguide_api.mealcrawl.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MenuImageAnalysisResponse(
        Long analysisLogId,
        MenuImageAnalysisResultSource resultSource,
        String identifiedFoodName,
        String identifiedFoodNameReason,
        BigDecimal imageConfidence,
        Long spicyLevel,
        List<MenuIngredientResponse> ingredients,
        List<MenuAllergyResponse> allergies,
        List<MatchedAllergyResponse> matchedAllergies,
        List<MatchedReligiousIngredientResponse> matchedReligiousIngredients
) {
    public enum MenuImageAnalysisResultSource {
        STORED_AI_ANALYSIS,
        LIVE_AI_ANALYSIS
    }

    public record MenuIngredientResponse(String code, String name) {
    }
    public record MenuAllergyResponse(String code, String name) {
    }
    public record MatchedAllergyResponse(String code, String name, String riskLevel, BigDecimal confidence) {
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
}

