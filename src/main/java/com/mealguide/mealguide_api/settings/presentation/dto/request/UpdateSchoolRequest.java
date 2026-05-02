package com.mealguide.mealguide_api.settings.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateSchoolRequest(
        @Schema(description = "학교 ID", example = "1")
        @NotNull
        @Positive
        Long schoolId
) {
}
