package com.mealguide.mealguide_api.settings.presentation.dto.response;

import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReligionOptionItemResponse(
        @Schema(description = "종교 식이 제한 코드", example = "HALAL")
        String code,
        @Schema(description = "사용자 설정 언어 기준 종교 식이 제한 이름", example = "Halal")
        String name
) {
    public static ReligionOptionItemResponse from(ReligiousRestrictionOption option) {
        return new ReligionOptionItemResponse(option.code(), option.name());
    }
}
