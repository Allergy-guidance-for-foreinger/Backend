package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CountryUpdateResponse(
        @Schema(description = "국가 코드", example = "KR")
        String countryCode
) {
}
