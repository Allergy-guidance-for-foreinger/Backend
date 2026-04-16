package com.mealguide.mealguide_api.global.auth.domain;

public record TokenClaims(
        Long userId,
        String deviceId,
        TokenType tokenType
) {
}

