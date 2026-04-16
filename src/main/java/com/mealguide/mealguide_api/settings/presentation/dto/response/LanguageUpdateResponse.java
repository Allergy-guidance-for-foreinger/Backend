package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LanguageUpdateResponse(
        @Schema(description = "변경된 ?�어 코드", example = "en")
        String languageCode
) {
}

