package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReligionUpdateResponse(
        @Schema(description = "변경된 종교???�이 ?�한 코드. ?�택 ?�제 ??null?�니??", example = "HALAL")
        String religiousCode
) {
}

