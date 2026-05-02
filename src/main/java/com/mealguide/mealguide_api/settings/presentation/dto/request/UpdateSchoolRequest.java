package com.mealguide.mealguide_api.settings.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateSchoolRequest(
        @Schema(description = "학교 ID", example = "1")
        @NotNull
        Long schoolId
) {
}
