package com.mealguide.mealguide_api.global.auth.port;

import com.mealguide.mealguide_api.global.auth.domain.AuthenticatedUser;
import com.mealguide.mealguide_api.global.auth.domain.TokenClaims;

public interface TokenProviderPort {
    String generateAccessToken(AuthenticatedUser user);

    String generateRefreshToken(AuthenticatedUser user);

    TokenClaims parseAccessToken(String token);

    TokenClaims parseRefreshToken(String token);

    long getAccessTokenExpirationSeconds();

    long getRefreshTokenExpirationSeconds();
}

