package com.mealguide.mealguide_api.mealcrawl.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MenuDetailBatchResponse(
        @Schema(description = "메뉴 상세 목록")
        List<MenuDetailResponse> menus
) {
}
