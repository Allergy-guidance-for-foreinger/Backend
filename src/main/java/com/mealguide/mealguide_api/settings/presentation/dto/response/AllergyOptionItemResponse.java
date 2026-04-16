package com.mealguide.mealguide_api.settings.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AllergyOptionItemResponse(
        @Schema(description = "?Œë ˆë¥´ê¸° ì½”ë“œ", example = "EGG")
        String code,
        @Schema(description = "?¬ìš©???¤ì • ?¸ì–´ ê¸°ì? ?Œë ˆë¥´ê¸° ?´ë¦„", example = "Egg")
        String name
) {
}


