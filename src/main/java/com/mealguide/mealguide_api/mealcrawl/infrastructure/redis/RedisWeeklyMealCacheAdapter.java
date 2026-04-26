package com.mealguide.mealguide_api.mealcrawl.infrastructure.redis;

import com.mealguide.mealguide_api.mealcrawl.application.port.WeeklyMealCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisWeeklyMealCacheAdapter implements WeeklyMealCachePort {

    private static final String WEEKLY_MEAL_CACHE_KEY_PREFIX = "meal:weekly:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String createWeeklyMealCacheKey(Long schoolId, Long cafeteriaId, LocalDate weekStartDate) {
        return buildKey(schoolId, cafeteriaId, weekStartDate);
    }

    @Override
    public Optional<String> findWeeklyMealCache(Long schoolId, Long cafeteriaId, LocalDate weekStartDate) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(buildKey(schoolId, cafeteriaId, weekStartDate)));
    }

    @Override
    public void upsertWeeklyMealCache(Long schoolId, Long cafeteriaId, LocalDate weekStartDate, String payload, Duration ttl) {
        validateTtl(ttl);
        stringRedisTemplate.opsForValue().set(buildKey(schoolId, cafeteriaId, weekStartDate), payload, ttl);
    }

    private String buildKey(Long schoolId, Long cafeteriaId, LocalDate weekStartDate) {
        return WEEKLY_MEAL_CACHE_KEY_PREFIX + schoolId + ":" + cafeteriaId + ":" + weekStartDate;
    }

    private void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be a positive duration");
        }
    }
}
