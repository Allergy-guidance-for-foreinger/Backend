package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.util.HashMap;
import java.util.Map;

public record WeeklyMealI18nCachePayload(
        Map<Long, String> menuNamesByMealMenuId
) {
    public WeeklyMealI18nCachePayload {
        if (menuNamesByMealMenuId == null || menuNamesByMealMenuId.isEmpty()) {
            menuNamesByMealMenuId = Map.of();
        } else {
            Map<Long, String> copied = new HashMap<>();
            menuNamesByMealMenuId.forEach((mealMenuId, name) -> {
                if (mealMenuId != null && name != null) {
                    copied.put(mealMenuId, name);
                }
            });
            menuNamesByMealMenuId = Map.copyOf(copied);
        }
    }
}
