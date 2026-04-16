package com.mealguide.mealguide_api.onboarding.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompleteOnboardingRequest(
        @Schema(description = "?�택???�어 코드", example = "en")
        @NotBlank
        String languageCode,

        @Schema(description = "?�택???�교 ID", example = "1")
        @NotNull
        Long schoolId,

        @Schema(description = "?�택???�레르기 코드 목록", example = "[\"EGG\", \"MILK\"]")
        @NotNull
        List<@NotBlank String> allergyCodes,

        @Schema(description = "?�택??종교 ?�이 ?�한 코드. 미선????null", example = "HALAL")
        String religiousCode
) {
}

