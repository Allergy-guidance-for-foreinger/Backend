package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CountryOptionItemResponse(
        @Schema(description = "국가 코드", example = "KR")
        String code,
        @Schema(description = "국가 이름", example = "Korea, Republic of")
        String name
) {
}
