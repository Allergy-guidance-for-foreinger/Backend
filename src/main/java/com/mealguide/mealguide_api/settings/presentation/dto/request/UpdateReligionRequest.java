package com.mealguide.mealguide_api.settings.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateReligionRequest(
        @Schema(description = "종교 식이 제한 코드. null이면 선택 해제입니다.", example = "HALAL")
        String religiousCode
) {
}