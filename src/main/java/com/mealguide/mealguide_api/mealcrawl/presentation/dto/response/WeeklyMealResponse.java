package com.mealguide.mealguide_api.mealcrawl.presentation.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WeeklyMealResponse(
        Long schoolId,
        Long cafeteriaId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        List<MealScheduleResponse> mealSchedules
) {
    public record MealScheduleResponse(
            LocalDate mealDate,
            String mealType,
            List<MenuResponse> menus
    ) {
    }

    public record MenuResponse(
            Long mealMenuId,
            String menuName,
            String cornerName,
            Integer displayOrder,
            Long spicyLevel,
            boolean aiAnalyzed,
            MenuRiskResponse risk
    ) {
    }

    public record MenuRiskResponse(
            String riskLevel,
            List<RiskReasonResponse> reasons
    ) {
    }

    public record RiskReasonResponse(
            String type,
            String code,
            String matchedIngredient,
            String source,
            String message
    ) {
    }
}
