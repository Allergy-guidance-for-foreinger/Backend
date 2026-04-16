package com.mealguide.mealguide_api.login.domain.google;

public record GoogleUserInfo(
        String subject,
        String email,
        String name,
        boolean emailVerified
) {
}

