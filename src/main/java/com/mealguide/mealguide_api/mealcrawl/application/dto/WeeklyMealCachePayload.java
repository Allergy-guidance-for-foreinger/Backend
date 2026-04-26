package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyMealCachePayload(
        Long schoolId,
        Long cafeteriaId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        List<MealScheduleItem> mealSchedules
) {
    public record MealScheduleItem(
            LocalDate mealDate,
            String mealType,
            List<MenuItem> menus
    ) {
    }

    public record MenuItem(
            Long mealMenuId,
            String menuName,
            String cornerName,
            Integer displayOrder,
            Long spicyLevel,
            boolean aiAnalyzed
    ) {
    }
}
