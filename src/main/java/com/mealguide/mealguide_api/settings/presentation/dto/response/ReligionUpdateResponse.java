package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReligionUpdateResponse(
        @Schema(description = "변경된 종교적 식이 제한 코드. 선택 해제 시 null입니다.", example = "HALAL")
        String religiousCode
) {
}
