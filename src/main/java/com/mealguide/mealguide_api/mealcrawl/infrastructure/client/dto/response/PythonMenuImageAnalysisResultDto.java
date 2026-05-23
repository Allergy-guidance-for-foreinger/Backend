package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.math.BigDecimal;

public record PythonMenuImageAnalysisResultDto(
        String identifiedFoodName,
        String identifiedFoodNameReason,
        BigDecimal confidence,
        String modelName,
        String modelVersion
) {
}

