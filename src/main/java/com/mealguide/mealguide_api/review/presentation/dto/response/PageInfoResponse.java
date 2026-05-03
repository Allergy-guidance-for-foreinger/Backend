package com.mealguide.mealguide_api.review.presentation.dto.response;

public record PageInfoResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
