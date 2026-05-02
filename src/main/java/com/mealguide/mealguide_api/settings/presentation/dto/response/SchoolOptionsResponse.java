package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SchoolOptionsResponse(
        @Schema(description = "전체 학교 목록")
        List<SchoolOptionItemResponse> schools
) {
}
