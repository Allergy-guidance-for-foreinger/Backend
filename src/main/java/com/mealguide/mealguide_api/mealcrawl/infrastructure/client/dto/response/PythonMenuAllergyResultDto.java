package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.math.BigDecimal;

public record PythonMenuAllergyResultDto(
        String allergyCode,
        BigDecimal confidence
) {
}
