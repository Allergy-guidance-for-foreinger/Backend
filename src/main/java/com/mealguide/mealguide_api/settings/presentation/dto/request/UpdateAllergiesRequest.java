package com.mealguide.mealguide_api.settings.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateAllergiesRequest(
        @Schema(description = "?�택???�레르기 코드 목록", example = "[\"EGG\", \"MILK\", \"SHRIMP\"]")
        @NotNull
        List<@NotBlank String> allergyCodes
) {
}

