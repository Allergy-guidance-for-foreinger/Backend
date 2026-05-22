package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ReligionUpdateResponse(
        @Schema(description = "변경된 종교 식이 제한 코드 목록", example = "[\"HALAL\", \"VEGAN\"]")
        List<String> religiousCodes
) {
}
