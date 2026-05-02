package com.mealguide.mealguide_api.settings.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateCountryRequest(
        @Schema(description = "국가 코드", example = "KR")
        @NotBlank
        String countryCode
) {
}
