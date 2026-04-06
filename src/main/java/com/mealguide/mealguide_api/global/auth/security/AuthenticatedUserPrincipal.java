package com.mealguide.mealguide_api.global.auth.security;

import com.mealguide.mealguide_api.global.auth.domain.AuthenticatedUser;
import com.mealguide.mealguide_api.login.domain.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

public record AuthenticatedUserPrincipal(
        Long userId,
        String email,
        String name,
        UserRole role,
        String deviceId,
        List<? extends GrantedAuthority> authorities
) implements Principal {

    public static AuthenticatedUserPrincipal from(AuthenticatedUser authenticatedUser) {
        return new AuthenticatedUserPrincipal(
                authenticatedUser.userId(),
                authenticatedUser.email(),
                authenticatedUser.name(),
                authenticatedUser.role(),
                authenticatedUser.deviceId(),
                List.of(new SimpleGrantedAuthority(toAuthority(authenticatedUser.role())))
        );
    }

    public static AuthenticatedUserPrincipal authenticated(Long userId, String deviceId) {
        return new AuthenticatedUserPrincipal(
                userId,
                null,
                null,
                null,
                deviceId,
                Collections.emptyList()
        );
    }

    private static String toAuthority(UserRole role) {
        if (role == null) {
            return "ROLE_USER";
        }
        return "ROLE_" + role.name();
    }

    @Override
    public String getName() {
        return email != null ? email : String.valueOf(userId);
    }
}
