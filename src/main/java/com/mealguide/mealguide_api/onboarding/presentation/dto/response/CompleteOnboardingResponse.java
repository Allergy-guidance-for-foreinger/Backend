package com.mealguide.mealguide_api.onboarding.presentation.dto.response;

import com.mealguide.mealguide_api.onboarding.domain.OnboardingCompletion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CompleteOnboardingResponse(
        @Schema(description = "Saved language code", example = "en")
        String languageCode,

        @Schema(description = "Saved school id", example = "1")
        Long schoolId,

        @Schema(description = "Saved allergy code list", example = "[\"EGG\", \"MILK\"]")
        List<String> allergyCodes,

        @Schema(description = "Saved religious restriction code. nullable.", example = "HALAL")
        String religiousCode,

        @Schema(description = "Onboarding completion flag", example = "true")
        boolean onboardingCompleted
) {
    public static CompleteOnboardingResponse from(OnboardingCompletion completion) {
        return new CompleteOnboardingResponse(
                completion.languageCode(),
                completion.schoolId(),
                completion.allergyCodes(),
                completion.religiousCode(),
                completion.onboardingCompleted()
        );
    }
}
