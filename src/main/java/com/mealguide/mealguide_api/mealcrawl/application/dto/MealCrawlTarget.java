package com.mealguide.mealguide_api.mealcrawl.application.dto;

import java.time.LocalDate;

public record MealCrawlTarget(
        Long schoolId,
        Long cafeteriaId,
        String schoolName,
        String cafeteriaName,
        String sourceUrl,
        LocalDate startDate,
        LocalDate endDate
) {
}


