package com.mealguide.mealguide_api.onboarding.presentation.dto.response;

import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SchoolListResponse(
        @Schema(description = "학교 목록")
        List<SchoolResponse> schools
) {
    public static SchoolListResponse from(List<SchoolOption> schools) {
        return new SchoolListResponse(schools.stream()
                .map(SchoolResponse::from)
                .toList());
    }

    public record SchoolResponse(
            @Schema(description = "학교 ID", example = "1")
            Long id,

            @Schema(description = "학교 이름", example = "Kumoh National Institute of Technology")
            String name
    ) {
        private static SchoolResponse from(SchoolOption school) {
            return new SchoolResponse(school.id(), school.name());
        }
    }
}
