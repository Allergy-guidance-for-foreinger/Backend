package com.mealguide.mealguide_api.review.presentation.dto.response;

import java.time.LocalDateTime;

public record ReviewCommentResponse(
        Long commentId,
        Long reviewId,
        String writerName,
        String content,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
