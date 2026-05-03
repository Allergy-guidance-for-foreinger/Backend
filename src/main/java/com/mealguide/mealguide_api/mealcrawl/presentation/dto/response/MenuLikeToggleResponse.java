package com.mealguide.mealguide_api.mealcrawl.presentation.dto.response;

public record MenuLikeToggleResponse(
        Long mealMenuId,
        Long cafeteriaId,
        Long menuId,
        long likeCount,
        boolean likedByMe
) {
}
