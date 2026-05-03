package com.mealguide.mealguide_api.review.presentation.dto.response;

import java.time.LocalDateTime;

public record ReviewCommentItemResponse(
        Long commentId,
        String writerName,
        String content,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
