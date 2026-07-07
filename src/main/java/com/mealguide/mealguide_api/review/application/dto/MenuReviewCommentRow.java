package com.mealguide.mealguide_api.review.application.dto;

import java.time.LocalDateTime;

public record MenuReviewCommentRow(
        Long commentId,
        Long reviewId,
        Long userId,
        String writerName,
        boolean writerDeleted,
        Long anonymousNo,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
