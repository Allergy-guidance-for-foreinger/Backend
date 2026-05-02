package com.mealguide.mealguide_api.settings.presentation.dto.response;

import com.mealguide.mealguide_api.settings.domain.SchoolOption;
import io.swagger.v3.oas.annotations.media.Schema;

public record SchoolOptionItemResponse(
        @Schema(description = "학교 ID", example = "1")
        Long schoolId,
        @Schema(description = "학교 이름", example = "금오공과대학교")
        String name
) {
    public static SchoolOptionItemResponse from(SchoolOption option) {
        return new SchoolOptionItemResponse(option.schoolId(), option.name());
    }
}
