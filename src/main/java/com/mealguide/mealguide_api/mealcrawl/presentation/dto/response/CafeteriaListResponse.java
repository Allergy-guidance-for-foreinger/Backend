package com.mealguide.mealguide_api.mealcrawl.presentation.dto.response;

import java.util.List;

public record CafeteriaListResponse(
        Long schoolId,
        List<CafeteriaItemResponse> cafeterias
) {
}
