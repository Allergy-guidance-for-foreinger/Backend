package com.mealguide.mealguide_api.review.presentation.dto.response;

public record ReviewLikeToggleResponse(
        Long reviewId,
        long likeCount,
        boolean likedByMe
) {
}
