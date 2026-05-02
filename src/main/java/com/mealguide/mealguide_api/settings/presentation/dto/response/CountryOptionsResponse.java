package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CountryOptionsResponse(
        @Schema(description = "전체 국가 목록")
        List<CountryOptionItemResponse> countries
) {
}
