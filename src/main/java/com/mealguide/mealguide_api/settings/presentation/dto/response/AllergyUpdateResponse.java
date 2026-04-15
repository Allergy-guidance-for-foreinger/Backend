package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AllergyUpdateResponse(
        @Schema(description = "변경된 알레르기 코드 목록", example = "[\"EGG\", \"MILK\"]")
        List<String> allergyCodes
) {
}
