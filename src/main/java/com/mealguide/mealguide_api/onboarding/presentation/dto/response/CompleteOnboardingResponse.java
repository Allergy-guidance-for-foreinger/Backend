package com.mealguide.mealguide_api.onboarding.presentation.dto.response;

import com.mealguide.mealguide_api.onboarding.domain.OnboardingCompletion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CompleteOnboardingResponse(
        @Schema(description = "저장된 언어 코드", example = "en")
        String languageCode,

        @Schema(description = "저장된 학교 ID", example = "1")
        Long schoolId,

        @Schema(description = "저장된 알레르기 코드 목록", example = "[\"EGG\", \"MILK\"]")
        List<String> allergyCodes,

        @Schema(description = "저장된 종교 식이 제한 코드. 미선택 시 null", example = "HALAL")
        String religiousCode,

        @Schema(description = "온보딩 완료 여부", example = "true")
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
