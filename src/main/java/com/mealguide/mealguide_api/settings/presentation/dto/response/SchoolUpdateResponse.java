package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SchoolUpdateResponse(
        @Schema(description = "학교 ID", example = "1")
        Long schoolId
) {
}
