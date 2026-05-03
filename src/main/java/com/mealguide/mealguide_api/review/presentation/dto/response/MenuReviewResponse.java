package com.mealguide.mealguide_api.review.presentation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MenuReviewResponse(
        Long reviewId,
        Long mealMenuId,
        Long cafeteriaId,
        Long menuId,
        String writerName,
        String content,
        LocalDate mealDate,
        long likeCount,
        long commentCount,
        boolean likedByMe,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
