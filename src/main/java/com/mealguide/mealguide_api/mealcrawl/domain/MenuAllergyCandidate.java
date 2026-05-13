package com.mealguide.mealguide_api.mealcrawl.domain;

import java.math.BigDecimal;

public record MenuAllergyCandidate(
        String allergyCode,
        BigDecimal confidence,
        String reason
) {
}

