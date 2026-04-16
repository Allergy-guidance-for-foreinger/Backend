package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.util.List;

public record MealImportResult(
        Long schoolId,
        Long cafeteriaId,
        List<Long> importedMenuIds,
        List<Long> menusNeedingAnalysis,
        List<Long> menusNeedingTranslation
) {
}


