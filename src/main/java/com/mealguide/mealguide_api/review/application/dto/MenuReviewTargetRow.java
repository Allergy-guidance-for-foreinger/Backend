package com.mealguide.mealguide_api.review.application.dto;

import java.time.LocalDate;

public record MenuReviewTargetRow(
        Long mealMenuId,
        Long cafeteriaId,
        Long menuId,
        LocalDate mealDate
) {
}
