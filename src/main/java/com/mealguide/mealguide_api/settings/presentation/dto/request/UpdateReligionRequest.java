package com.mealguide.mealguide_api.settings.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateReligionRequest(
        @Schema(description = "ì¢…êµ???ì´ ?œí•œ ì½”ë“œ. null?´ë©´ ? íƒ???´ì œ?©ë‹ˆ??", example = "HALAL")
        String religiousCode
) {
}

