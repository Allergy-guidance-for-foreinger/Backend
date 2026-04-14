package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record LanguageOptionsResponse(
        @Schema(description = "전체 언어 목록")
        List<LanguageOptionItemResponse> languages
) {
}

