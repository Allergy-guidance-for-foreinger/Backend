package com.mealguide.mealguide_api.mealcrawl.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailBaseCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRiskDataCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.ReligionIngredientMapCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealI18nCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealRiskDataCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuReadCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMenuReadCacheAdapter implements MenuReadCachePort {

    private static final String WEEKLY_RISK_DATA_PREFIX = "meal:weekly:risk-data:";
    private static final String WEEKLY_I18N_PREFIX = "meal:weekly:i18n:";
    private static final String MENU_DETAIL_BASE_PREFIX = "menu:detail:base:";
    private static final String MENU_DETAIL_RISK_PREFIX = "menu:detail:risk-data:";
    private static final String RELIGION_INGREDIENT_MAP_KEY = "religion:ingredient-map";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<WeeklyMealRiskDataCachePayload> findWeeklyMealRiskData(Long cafeteriaId, LocalDate weekStartDate) {
        return read(buildWeeklyRiskDataKey(cafeteriaId, weekStartDate), WeeklyMealRiskDataCachePayload.class);
    }

    @Override
    public void upsertWeeklyMealRiskData(Long cafeteriaId, LocalDate weekStartDate, WeeklyMealRiskDataCachePayload payload, Duration ttl) {
        write(buildWeeklyRiskDataKey(cafeteriaId, weekStartDate), payload, ttl);
    }

    @Override
    public Optional<WeeklyMealI18nCachePayload> findWeeklyMealI18n(Long cafeteriaId, LocalDate weekStartDate, String langCode) {
        return read(buildWeeklyI18nKey(cafeteriaId, weekStartDate, langCode), WeeklyMealI18nCachePayload.class);
    }

    @Override
    public void upsertWeeklyMealI18n(Long cafeteriaId, LocalDate weekStartDate, String langCode, WeeklyMealI18nCachePayload payload, Duration ttl) {
        write(buildWeeklyI18nKey(cafeteriaId, weekStartDate, langCode), payload, ttl);
    }

    @Override
    public Optional<MenuDetailBaseCachePayload> findMenuDetailBase(Long mealMenuId, String langCode) {
        return read(buildMenuDetailBaseKey(mealMenuId, langCode), MenuDetailBaseCachePayload.class);
    }

    @Override
    public void upsertMenuDetailBase(Long mealMenuId, String langCode, MenuDetailBaseCachePayload payload, Duration ttl) {
        write(buildMenuDetailBaseKey(mealMenuId, langCode), payload, ttl);
    }

    @Override
    public Optional<MenuDetailRiskDataCachePayload> findMenuDetailRiskData(Long mealMenuId) {
        return read(buildMenuDetailRiskKey(mealMenuId), MenuDetailRiskDataCachePayload.class);
    }

    @Override
    public void upsertMenuDetailRiskData(Long mealMenuId, MenuDetailRiskDataCachePayload payload, Duration ttl) {
        write(buildMenuDetailRiskKey(mealMenuId), payload, ttl);
    }

    @Override
    public Optional<ReligionIngredientMapCachePayload> findReligionIngredientMap() {
        return read(RELIGION_INGREDIENT_MAP_KEY, ReligionIngredientMapCachePayload.class);
    }

    @Override
    public void upsertReligionIngredientMap(ReligionIngredientMapCachePayload payload, Duration ttl) {
        write(RELIGION_INGREDIENT_MAP_KEY, payload, ttl);
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(value, type));
        } catch (Exception exception) {
            log.warn("Read cache lookup failed. key={}", key, exception);
            return Optional.empty();
        }
    }

    private void write(String key, Object payload, Duration ttl) {
        try {
            validateTtl(ttl);
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(payload), ttl);
        } catch (JsonProcessingException exception) {
            log.warn("Read cache serialization failed. key={}", key, exception);
        } catch (Exception exception) {
            log.warn("Read cache write failed. key={}", key, exception);
        }
    }

    private String buildWeeklyRiskDataKey(Long cafeteriaId, LocalDate weekStartDate) {
        return WEEKLY_RISK_DATA_PREFIX + cafeteriaId + ":" + weekStartDate;
    }

    private String buildWeeklyI18nKey(Long cafeteriaId, LocalDate weekStartDate, String langCode) {
        return WEEKLY_I18N_PREFIX + cafeteriaId + ":" + weekStartDate + ":" + normalizeLangCode(langCode);
    }

    private String buildMenuDetailBaseKey(Long mealMenuId, String langCode) {
        return MENU_DETAIL_BASE_PREFIX + mealMenuId + ":" + normalizeLangCode(langCode);
    }

    private String buildMenuDetailRiskKey(Long mealMenuId) {
        return MENU_DETAIL_RISK_PREFIX + mealMenuId;
    }

    private String normalizeLangCode(String langCode) {
        return langCode == null || langCode.isBlank() ? "ko" : langCode.trim().toLowerCase();
    }

    private void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be a positive duration");
        }
    }
}
