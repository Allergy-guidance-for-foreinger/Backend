package com.mealguide.mealguide_api.review.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MenuReviewRow(
        Long reviewId,
        Long userId,
        String writerName,
        boolean writerDeleted,
        Long cafeteriaId,
        Long menuId,
        Long mealMenuId,
        LocalDate mealDate,
        String content,
        long likeCount,
        long commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
