package com.mealguide.mealguide_api.login.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "refreshToken is required.")
        String refreshToken
) {
}

