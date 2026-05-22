package com.mealguide.mealguide_api.settings.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateReligionRequest(
        @Schema(description = "종교 식이 제한 코드 목록. 빈 배열이면 선택 해제입니다.", example = "[\"HALAL\", \"VEGAN\"]")
        @NotNull
        List<@NotBlank String> religiousCodes
) {
}
