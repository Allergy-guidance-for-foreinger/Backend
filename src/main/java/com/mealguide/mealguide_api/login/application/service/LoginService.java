package com.mealguide.mealguide_api.login.application.service;

import com.mealguide.mealguide_api.global.auth.domain.AuthenticatedUser;
import com.mealguide.mealguide_api.global.auth.domain.TokenClaims;
import com.mealguide.mealguide_api.global.auth.port.RefreshTokenPort;
import com.mealguide.mealguide_api.global.auth.port.TokenProviderPort;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.login.application.port.GoogleIdTokenVerifierPort;
import com.mealguide.mealguide_api.login.application.port.UserQueryPort;
import com.mealguide.mealguide_api.global.auth.jwt.dto.AuthTokenResult;
import com.mealguide.mealguide_api.login.domain.google.GoogleUserInfo;
import com.mealguide.mealguide_api.login.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final GoogleIdTokenVerifierPort googleIdTokenVerifierPort;
    private final UserQueryPort userQueryPort;
    private final RefreshTokenPort refreshTokenPort;
    private final TokenProviderPort tokenProviderPort;

    @Transactional
    public AuthTokenResult login(String idToken, String deviceId) {
        GoogleUserInfo googleUserInfo = googleIdTokenVerifierPort.verify(idToken);
        return loginVerifiedGoogleUser(googleUserInfo, deviceId);
    }

    @Transactional
    public AuthTokenResult loginVerifiedGoogleUser(GoogleUserInfo googleUserInfo, String deviceId) {
        if (!googleUserInfo.emailVerified()) {
            throw new ServiceException(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
        }

        User user = userQueryPort.findByGoogleAccount(googleUserInfo.subject(), googleUserInfo.email())
                .orElseGet(() -> userQueryPort.createGoogleUser(
                        googleUserInfo.subject(),
                        googleUserInfo.email(),
                        googleUserInfo.name()
                ));

        return issueTokens(AuthenticatedUser.from(user, deviceId));
    }

    @Transactional
    public AuthTokenResult refresh(String refreshToken) {
        TokenClaims tokenClaims = tokenProviderPort.parseRefreshToken(refreshToken);
        User user = userQueryPort.findById(tokenClaims.userId())
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user, tokenClaims.deviceId());
        String newAccessToken = tokenProviderPort.generateAccessToken(authenticatedUser);
        String newRefreshToken = tokenProviderPort.generateRefreshToken(authenticatedUser);
        long refreshTokenTtl = tokenProviderPort.getRefreshTokenExpirationSeconds();

        boolean rotated = refreshTokenPort.rotateIfMatch(
                tokenClaims.userId(),
                tokenClaims.deviceId(),
                refreshToken,
                newRefreshToken,
                Duration.ofSeconds(refreshTokenTtl)
        );

        if (!rotated) {
            throw new ServiceException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        return new AuthTokenResult(
                newAccessToken,
                newRefreshToken,
                tokenProviderPort.getAccessTokenExpirationSeconds(),
                refreshTokenTtl
        );
    }

    public void logout(Long authenticatedUserId, String refreshToken) {
        TokenClaims tokenClaims = tokenProviderPort.parseRefreshToken(refreshToken);
        if (!authenticatedUserId.equals(tokenClaims.userId()) || tokenClaims.deviceId() == null || tokenClaims.deviceId().isBlank()) {
            throw new ServiceException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        refreshTokenPort.deleteByUserIdAndDeviceId(authenticatedUserId, tokenClaims.deviceId());
    }

    private AuthTokenResult issueTokens(AuthenticatedUser authenticatedUser) {
        String accessToken = tokenProviderPort.generateAccessToken(authenticatedUser);
        String refreshToken = tokenProviderPort.generateRefreshToken(authenticatedUser);
        long refreshTokenTtl = tokenProviderPort.getRefreshTokenExpirationSeconds();

        refreshTokenPort.save(authenticatedUser.userId(), authenticatedUser.deviceId(), refreshToken, Duration.ofSeconds(refreshTokenTtl));

        return new AuthTokenResult(
                accessToken,
                refreshToken,
                tokenProviderPort.getAccessTokenExpirationSeconds(),
                refreshTokenTtl
        );
    }
}
