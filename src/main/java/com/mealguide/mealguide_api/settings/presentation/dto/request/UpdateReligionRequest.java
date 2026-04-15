package com.mealguide.mealguide_api.settings.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateReligionRequest(
        @Schema(description = "종교적 식이 제한 코드. null이면 선택을 해제합니다.", example = "HALAL")
        String religiousCode
) {
}
