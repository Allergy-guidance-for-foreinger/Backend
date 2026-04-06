package com.mealguide.mealguide_api.global.auth.port;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenPort {
    void save(Long userId, String deviceId, String refreshToken, Duration ttl);

    Optional<String> findByUserIdAndDeviceId(Long userId, String deviceId);

    boolean rotateIfMatch(Long userId, String deviceId, String expectedRefreshToken, String newRefreshToken, Duration ttl);

    void deleteByUserIdAndDeviceId(Long userId, String deviceId);
}
