package com.mealguide.mealguide_api.mealcrawl.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MenuDetailBatchRequest(
        @Schema(description = "조회할 mealMenuId 목록 (최대 30개)", example = "[101, 102, 103]")
        @NotEmpty
        @Size(max = 30)
        List<@NotNull @Positive Long> mealMenuIds
) {
}
