package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LanguageOptionItemResponse(
        @Schema(description = "?∏Ïñ¥ ÏΩîÎìú", example = "en")
        String code,
        @Schema(description = "Í∏∞Î≥∏ ?∏Ïñ¥ ?¥Î¶Ñ", example = "?ÅÏñ¥")
        String name,
        @Schema(description = "?ÅÎ¨∏ ?∏Ïñ¥ ?¥Î¶Ñ", example = "English")
        String englishName
) {
}


