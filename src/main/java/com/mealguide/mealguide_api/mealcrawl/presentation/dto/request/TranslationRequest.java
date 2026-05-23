package com.mealguide.mealguide_api.mealcrawl.presentation.dto.request;

import com.mealguide.mealguide_api.mealcrawl.presentation.validation.ValidTranslationLanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TranslationRequest(
        @Schema(description = "원문 언어 코드 (code only: ko, en)", example = "ko")
        @NotBlank
        @ValidTranslationLanguageCode
        String sourceLang,

        @Schema(description = "번역 대상 언어 코드 (code only: ko, en)", example = "en")
        @NotBlank
        @ValidTranslationLanguageCode
        String targetLang,

        @Schema(description = "번역할 원문 텍스트", example = "안녕하세요")
        @NotBlank
        String text
) {
}
