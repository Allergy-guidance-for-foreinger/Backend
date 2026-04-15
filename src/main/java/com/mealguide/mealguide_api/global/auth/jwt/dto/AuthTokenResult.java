package com.mealguide.mealguide_api.global.auth.jwt.dto;

public record AuthTokenResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        Boolean onboardingCompleted
) {
}
