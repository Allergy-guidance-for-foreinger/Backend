package com.mealguide.mealguide_api.onboarding.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompleteOnboardingRequest(
        @Schema(description = "선택한 언어 코드", example = "en")
        @NotBlank
        String languageCode,

        @Schema(description = "선택한 학교 ID", example = "1")
        @NotNull
        Long schoolId,

        @Schema(description = "선택한 알레르기 코드 목록", example = "[\"EGG\", \"MILK\"]")
        @NotNull
        List<@NotBlank String> allergyCodes,

        @Schema(description = "선택한 종교 식이 제한 코드. 미선택 시 null", example = "HALAL")
        String religiousCode
) {
}
