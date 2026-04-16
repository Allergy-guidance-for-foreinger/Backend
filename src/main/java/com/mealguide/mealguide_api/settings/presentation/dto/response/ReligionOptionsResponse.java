package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ReligionOptionsResponse(
        @Schema(description = "?�체 종교???�이 ?�한 목록")
        List<ReligionOptionItemResponse> religions
) {
}


