package com.mealguide.mealguide_api.global.auth.domain;

import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserRole;

public record AuthenticatedUser(
        Long userId,
        String email,
        String name,
        UserRole role,
        String deviceId
) {
    public static AuthenticatedUser from(User user, String deviceId) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getName(), user.getRole(), deviceId);
    }
}

