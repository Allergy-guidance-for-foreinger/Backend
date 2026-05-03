package com.mealguide.mealguide_api.review.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReviewCommentRequest(
        @NotBlank
        @Size(max = 500)
        String content
) {
}
