package com.mealguide.mealguide_api.onboarding.presentation.dto.response;

import com.mealguide.mealguide_api.onboarding.domain.OnboardingCompletion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CompleteOnboardingResponse(
        @Schema(description = "?€?¥ëœ ?¸ì–´ ì½”ë“œ", example = "en")
        String languageCode,

        @Schema(description = "?€?¥ëœ ?™êµ ID", example = "1")
        Long schoolId,

        @Schema(description = "?€?¥ëœ ?Œë ˆë¥´ê¸° ì½”ë“œ ëª©ë¡", example = "[\"EGG\", \"MILK\"]")
        List<String> allergyCodes,

        @Schema(description = "?€?¥ëœ ì¢…êµ ?ì´ ?œí•œ ì½”ë“œ. ë¯¸ì„ ????null", example = "HALAL")
        String religiousCode,

        @Schema(description = "?¨ë³´???„ë£Œ ?¬ë?", example = "true")
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

