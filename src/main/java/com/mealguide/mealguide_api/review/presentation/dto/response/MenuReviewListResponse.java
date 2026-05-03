package com.mealguide.mealguide_api.review.presentation.dto.response;

import java.util.List;

public record MenuReviewListResponse(
        Long mealMenuId,
        Long cafeteriaId,
        Long menuId,
        long reviewCount,
        List<MenuReviewItemResponse> reviews,
        PageInfoResponse page
) {
}
