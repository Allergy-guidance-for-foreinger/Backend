package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.util.List;
import java.util.Map;

public record ReligionIngredientMapCachePayload(
        Map<String, List<RestrictionData>> restrictionsByIngredientCode
) {
    public record RestrictionData(
            String restrictionCode,
            Map<String, String> namesByLangCode
    ) {
    }
}
