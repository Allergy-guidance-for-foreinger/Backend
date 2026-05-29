package com.mealguide.mealguide_api.mealcrawl.presentation.dto.response;

import java.time.OffsetDateTime;

public record MenuImageAnalysisUsageResponse(
        long usedCount,
        int limitCount,
        long remainingCount,
        boolean limited,
        OffsetDateTime resetAt
) {
}
