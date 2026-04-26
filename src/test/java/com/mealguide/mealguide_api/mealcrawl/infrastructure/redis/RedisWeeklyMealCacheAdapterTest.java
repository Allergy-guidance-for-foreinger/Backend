package com.mealguide.mealguide_api.mealcrawl.infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisWeeklyMealCacheAdapterTest {

    @Test
    void createWeeklyMealCacheKeyBuildsExpectedFormat() {
        RedisWeeklyMealCacheAdapter adapter = new RedisWeeklyMealCacheAdapter(mock(StringRedisTemplate.class));

        String key = adapter.createWeeklyMealCacheKey(1L, 10L, LocalDate.of(2026, 4, 20));

        assertThat(key).isEqualTo("meal:weekly:1:10:2026-04-20");
    }

    @Test
    void upsertWeeklyMealCacheUsesSetWithTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RedisWeeklyMealCacheAdapter adapter = new RedisWeeklyMealCacheAdapter(redisTemplate);
        Duration ttl = Duration.ofHours(24);

        adapter.upsertWeeklyMealCache(1L, 10L, LocalDate.of(2026, 4, 20), "{\"ok\":true}", ttl);

        verify(valueOperations).set("meal:weekly:1:10:2026-04-20", "{\"ok\":true}", ttl);
    }
}
