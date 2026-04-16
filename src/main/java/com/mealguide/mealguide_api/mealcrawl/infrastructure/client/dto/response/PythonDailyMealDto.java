package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PythonDailyMealDto(
        LocalDate mealDate,
        String mealType,
        List<PythonCrawledMenuDto> menus
) {
}



