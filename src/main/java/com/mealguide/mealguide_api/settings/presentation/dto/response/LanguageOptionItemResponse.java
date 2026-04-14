package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LanguageOptionItemResponse(
        @Schema(description = "언어 코드", example = "en")
        String code,
        @Schema(description = "기본 언어 이름", example = "영어")
        String name,
        @Schema(description = "영문 언어 이름", example = "English")
        String englishName
) {
}

