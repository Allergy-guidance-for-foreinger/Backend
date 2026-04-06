package com.mealguide.mealguide_api.global.auth.redis;

import com.mealguide.mealguide_api.global.auth.port.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenAdapter implements RefreshTokenPort {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final DefaultRedisScript<Long> ROTATE_IF_MATCH_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if current == ARGV[1] then
                redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2])
                return 1
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(Long userId, String deviceId, String refreshToken, Duration ttl) {
        stringRedisTemplate.opsForValue().set(buildKey(userId, deviceId), hashToken(refreshToken), ttl);
    }

    @Override
    public Optional<String> findByUserIdAndDeviceId(Long userId, String deviceId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(buildKey(userId, deviceId)));
    }

    @Override
    public boolean rotateIfMatch(Long userId, String deviceId, String expectedRefreshToken, String newRefreshToken, Duration ttl) {
        Long result = stringRedisTemplate.execute(
                ROTATE_IF_MATCH_SCRIPT,
                List.of(buildKey(userId, deviceId)),
                hashToken(expectedRefreshToken),
                hashToken(newRefreshToken),
                String.valueOf(ttl.toMillis())
        );
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void deleteByUserIdAndDeviceId(Long userId, String deviceId) {
        stringRedisTemplate.delete(buildKey(userId, deviceId));
    }

    private String buildKey(Long userId, String deviceId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId + ":" + deviceId;
    }

    private String hashToken(String refreshToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
