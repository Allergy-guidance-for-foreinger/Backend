package com.mealguide.mealguide_api.global.auth.redis;

import com.mealguide.mealguide_api.global.auth.port.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenAdapter implements RefreshTokenPort {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(Long userId, String deviceId, String refreshToken, Duration ttl) {
        stringRedisTemplate.opsForValue().set(buildKey(userId, deviceId), refreshToken, ttl);
    }

    @Override
    public Optional<String> findByUserIdAndDeviceId(Long userId, String deviceId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(buildKey(userId, deviceId)));
    }

    @Override
    public void deleteByUserIdAndDeviceId(Long userId, String deviceId) {
        stringRedisTemplate.delete(buildKey(userId, deviceId));
    }

    private String buildKey(Long userId, String deviceId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId + ":" + deviceId;
    }
}
