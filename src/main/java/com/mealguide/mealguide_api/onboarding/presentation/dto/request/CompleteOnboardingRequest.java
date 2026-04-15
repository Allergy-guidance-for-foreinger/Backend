package com.mealguide.mealguide_api.onboarding.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompleteOnboardingRequest(
        @Schema(description = "Selected language code", example = "en")
        @NotBlank
        String languageCode,

        @Schema(description = "Selected school id", example = "1")
        @NotNull
        Long schoolId,

        @Schema(description = "Selected allergy code list", example = "[\"EGG\", \"MILK\"]")
        @NotNull
        List<@NotBlank String> allergyCodes,

        @Schema(description = "Selected religious restriction code. nullable.", example = "HALAL")
        String religiousCode
) {
}
