package com.mealguide.mealguide_api.login.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "idToken is required.")
        String idToken,

        @NotBlank(message = "deviceId is required.")
        String deviceId
) {
}
