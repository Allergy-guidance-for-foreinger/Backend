package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.util.Map;

public record WeeklyMealI18nCachePayload(
        Map<Long, String> menuNamesByMealMenuId
) {
    public WeeklyMealI18nCachePayload {
        menuNamesByMealMenuId = menuNamesByMealMenuId == null ? Map.of() : Map.copyOf(menuNamesByMealMenuId);
    }
}
