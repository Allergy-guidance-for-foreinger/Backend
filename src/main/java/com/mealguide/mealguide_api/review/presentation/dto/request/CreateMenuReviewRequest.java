package com.mealguide.mealguide_api.review.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMenuReviewRequest(
        @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Size(max = 1000, message = "리뷰 내용은 1000자를 초과할 수 없습니다.")
        String content
) {
}
