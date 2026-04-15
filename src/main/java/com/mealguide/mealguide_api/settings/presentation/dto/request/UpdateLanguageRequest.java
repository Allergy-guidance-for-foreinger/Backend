package com.mealguide.mealguide_api.settings.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateLanguageRequest(
        @Schema(description = "언어 코드", example = "en")
        @NotBlank
        String languageCode
) {
}
