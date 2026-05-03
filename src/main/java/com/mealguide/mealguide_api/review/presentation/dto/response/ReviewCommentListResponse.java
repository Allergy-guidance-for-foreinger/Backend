package com.mealguide.mealguide_api.review.presentation.dto.response;

import java.util.List;

public record ReviewCommentListResponse(
        Long reviewId,
        List<ReviewCommentItemResponse> comments,
        PageInfoResponse page
) {
}
