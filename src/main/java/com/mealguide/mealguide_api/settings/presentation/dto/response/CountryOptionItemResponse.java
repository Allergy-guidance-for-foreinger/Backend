package com.mealguide.mealguide_api.settings.presentation.dto.response;

import com.mealguide.mealguide_api.settings.domain.CountryOption;
import io.swagger.v3.oas.annotations.media.Schema;

public record CountryOptionItemResponse(
        @Schema(description = "국가 코드", example = "KR")
        String code,
        @Schema(description = "국가 이름", example = "Korea, Republic of")
        String name
) {
    public static CountryOptionItemResponse from(CountryOption option) {
        return new CountryOptionItemResponse(option.code(), option.name());
    }
}
