package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReligionOptionItemResponse(
        @Schema(description = "ì¢…êµ???ì´ ?œí•œ ì½”ë“œ", example = "HALAL")
        String code,
        @Schema(description = "?¬ìš©???¤ì • ?¸ì–´ ê¸°ì? ì¢…êµ???ì´ ?œí•œ ?´ë¦„", example = "Halal")
        String name
) {
}


