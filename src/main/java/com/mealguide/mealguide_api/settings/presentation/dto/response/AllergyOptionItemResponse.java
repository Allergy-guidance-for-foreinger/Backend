package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AllergyOptionItemResponse(
        @Schema(description = "알레르기 코드", example = "EGG")
        String code,
        @Schema(description = "사용자 설정 언어 기준 알레르기 이름", example = "Egg")
        String name
) {
}

