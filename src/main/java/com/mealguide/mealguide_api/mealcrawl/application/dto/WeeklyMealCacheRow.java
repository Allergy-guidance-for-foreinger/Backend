package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.time.LocalDate;

public record WeeklyMealCacheRow(
        LocalDate mealDate,
        String mealType,
        Integer displayOrder,
        String cornerName,
        Long mealMenuId,
        String menuName,
        Long spicyLevel,
        String aiAnalysisStatus
) {
}
