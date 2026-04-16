package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request;

import java.time.LocalDate;

public record PythonMealCrawlRequest(
        String schoolName,
        String cafeteriaName,
        String sourceUrl,
        LocalDate startDate,
        LocalDate endDate
) {
}



