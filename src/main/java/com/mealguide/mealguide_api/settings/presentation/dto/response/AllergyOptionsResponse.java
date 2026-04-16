package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AllergyOptionsResponse(
        @Schema(description = "?�체 ?�레르기 목록")
        List<AllergyOptionItemResponse> allergies
) {
}


