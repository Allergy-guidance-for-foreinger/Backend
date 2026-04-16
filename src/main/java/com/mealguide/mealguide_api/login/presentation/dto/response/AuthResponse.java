package com.mealguide.mealguide_api.login.presentation.dto.response;

import com.mealguide.mealguide_api.global.auth.jwt.dto.AuthTokenResult;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        long refreshExpiresIn,
        Boolean onboardingCompleted
) {
    public static AuthResponse from(AuthTokenResult authTokenResult) {
        return new AuthResponse(
                authTokenResult.accessToken(),
                authTokenResult.refreshToken(),
                authTokenResult.accessTokenExpiresIn(),
                authTokenResult.refreshTokenExpiresIn(),
                authTokenResult.onboardingCompleted()
        );
    }
}

