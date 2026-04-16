package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PythonMealCrawlResponse(
        String schoolName,
        String cafeteriaName,
        String sourceUrl,
        LocalDate startDate,
        LocalDate endDate,
        List<PythonDailyMealDto> meals
) {
}



