package com.mealguide.mealguide_api.login.application.service;

import com.mealguide.mealguide_api.global.auth.domain.TokenClaims;
import com.mealguide.mealguide_api.global.auth.domain.TokenType;
import com.mealguide.mealguide_api.global.auth.port.RefreshTokenPort;
import com.mealguide.mealguide_api.global.auth.port.TokenProviderPort;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.login.application.port.GoogleIdTokenVerifierPort;
import com.mealguide.mealguide_api.login.application.port.UserQueryPort;
import com.mealguide.mealguide_api.global.auth.jwt.dto.AuthTokenResult;
import com.mealguide.mealguide_api.login.domain.google.GoogleUserInfo;
import com.mealguide.mealguide_api.login.domain.User;
import com.mealguide.mealguide_api.login.domain.UserRole;
import com.mealguide.mealguide_api.login.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private final GoogleIdTokenVerifierPort googleIdTokenVerifierPort = mock(GoogleIdTokenVerifierPort.class);
    private final UserQueryPort userQueryPort = mock(UserQueryPort.class);
    private final TokenProviderPort tokenProviderPort = mock(TokenProviderPort.class);
    private final InMemoryRefreshTokenPort refreshTokenPort = new InMemoryRefreshTokenPort();

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(googleIdTokenVerifierPort, userQueryPort, refreshTokenPort, tokenProviderPort);
        when(tokenProviderPort.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(tokenProviderPort.getRefreshTokenExpirationSeconds()).thenReturn(1209600L);
    }

    @Test
    void googleLoginSuccessIssuesAccessAndRefreshTokens() {
        User user = createUser(1L, "user@test.com", UserRole.USER);
        String deviceId = "device-001";

        when(googleIdTokenVerifierPort.verify("google-id-token"))
                .thenReturn(new GoogleUserInfo("google-sub", "user@test.com", "Meal Guide", true));
        when(userQueryPort.findByGoogleAccount("google-sub", "user@test.com")).thenReturn(Optional.of(user));
        when(tokenProviderPort.generateAccessToken(any())).thenReturn("access-token");
        when(tokenProviderPort.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthTokenResult result = loginService.login("google-id-token", deviceId);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(refreshTokenPort.findByUserIdAndDeviceId(1L, deviceId)).contains(hashToken("refresh-token"));
    }

    @Test
    void googleLoginCreatesUserWhenUserDoesNotExist() {
        String deviceId = "device-001";
        User createdUser = createUser(2L, "missing@test.com", UserRole.USER);

        when(googleIdTokenVerifierPort.verify("google-id-token"))
                .thenReturn(new GoogleUserInfo("google-sub", "missing@test.com", "Meal Guide", true));
        when(userQueryPort.findByGoogleAccount("google-sub", "missing@test.com")).thenReturn(Optional.empty());
        when(userQueryPort.createGoogleUser("google-sub", "missing@test.com", "Meal Guide")).thenReturn(createdUser);
        when(tokenProviderPort.generateAccessToken(any())).thenReturn("access-token");
        when(tokenProviderPort.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthTokenResult result = loginService.login("google-id-token", deviceId);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(refreshTokenPort.findByUserIdAndDeviceId(2L, deviceId)).contains(hashToken("refresh-token"));
    }

    @Test
    void refreshSuccessRotatesRefreshToken() {
        User user = createUser(1L, "user@test.com", UserRole.USER);
        String deviceId = "device-001";
        refreshTokenPort.save(1L, deviceId, "old-refresh-token", Duration.ofDays(14));

        when(tokenProviderPort.parseRefreshToken("old-refresh-token"))
                .thenReturn(new TokenClaims(1L, deviceId, TokenType.REFRESH));
        when(userQueryPort.findById(1L)).thenReturn(Optional.of(user));
        when(tokenProviderPort.generateAccessToken(any())).thenReturn("new-access-token");
        when(tokenProviderPort.generateRefreshToken(any())).thenReturn("new-refresh-token");

        AuthTokenResult result = loginService.refresh("old-refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(refreshTokenPort.findByUserIdAndDeviceId(1L, deviceId)).contains(hashToken("new-refresh-token"));
    }

    @Test
    void refreshFailsWhenRefreshTokenDoesNotExistInRedis() {
        String deviceId = "device-001";

        when(tokenProviderPort.parseRefreshToken("missing-refresh-token"))
                .thenReturn(new TokenClaims(1L, deviceId, TokenType.REFRESH));

        assertThatThrownBy(() -> loginService.refresh("missing-refresh-token"))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    void refreshFailsAfterLogout() {
        String deviceId = "device-001";
        refreshTokenPort.save(1L, deviceId, "refresh-token", Duration.ofDays(14));

        when(tokenProviderPort.parseRefreshToken("refresh-token"))
                .thenReturn(new TokenClaims(1L, deviceId, TokenType.REFRESH));

        loginService.logout(1L, "refresh-token");

        assertThat(refreshTokenPort.findByUserIdAndDeviceId(1L, deviceId)).isEmpty();
        assertThatThrownBy(() -> loginService.refresh("refresh-token"))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    private User createUser(Long id, String email, UserRole role) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "schoolId", 100L);
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "name", "Meal Guide");
        ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }

    private static class InMemoryRefreshTokenPort implements RefreshTokenPort {
        private final Map<String, String> storage = new HashMap<>();

        @Override
        public void save(Long userId, String deviceId, String refreshToken, Duration ttl) {
            storage.put(buildKey(userId, deviceId), hashToken(refreshToken));
        }

        @Override
        public Optional<String> findByUserIdAndDeviceId(Long userId, String deviceId) {
            return Optional.ofNullable(storage.get(buildKey(userId, deviceId)));
        }

        @Override
        public boolean rotateIfMatch(Long userId, String deviceId, String expectedRefreshToken, String newRefreshToken, Duration ttl) {
            String key = buildKey(userId, deviceId);
            String current = storage.get(key);
            if (!hashToken(expectedRefreshToken).equals(current)) {
                return false;
            }
            storage.put(key, hashToken(newRefreshToken));
            return true;
        }

        @Override
        public void deleteByUserIdAndDeviceId(Long userId, String deviceId) {
            storage.remove(buildKey(userId, deviceId));
        }

        private String buildKey(Long userId, String deviceId) {
            return userId + ":" + deviceId;
        }
    }

    private static String hashToken(String refreshToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
