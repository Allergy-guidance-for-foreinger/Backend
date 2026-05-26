package com.mealguide.mealguide_api.global.auth.domain;

import com.mealguide.mealguide_api.login.domain.UserRole;

public record TokenClaims(
        Long userId,
        String deviceId,
        UserRole role,
        TokenType tokenType
) {
}

