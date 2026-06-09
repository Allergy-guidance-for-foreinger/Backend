package com.mealguide.mealguide_api.mealcrawl.application.port;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailBaseCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRiskDataCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.ReligionIngredientMapCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealI18nCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealRiskDataCachePayload;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MenuReadCachePort {

    Optional<WeeklyMealRiskDataCachePayload> findWeeklyMealRiskData(Long cafeteriaId, LocalDate weekStartDate);

    void upsertWeeklyMealRiskData(Long cafeteriaId, LocalDate weekStartDate, WeeklyMealRiskDataCachePayload payload, Duration ttl);

    Optional<WeeklyMealI18nCachePayload> findWeeklyMealI18n(Long cafeteriaId, LocalDate weekStartDate, String langCode);

    void upsertWeeklyMealI18n(Long cafeteriaId, LocalDate weekStartDate, String langCode, WeeklyMealI18nCachePayload payload, Duration ttl);

    Optional<MenuDetailBaseCachePayload> findMenuDetailBase(Long mealMenuId, String langCode);

    default Map<Long, MenuDetailBaseCachePayload> findMenuDetailBases(Set<Long> mealMenuIds, String langCode) {
        Map<Long, MenuDetailBaseCachePayload> result = new LinkedHashMap<>();
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return result;
        }
        for (Long mealMenuId : mealMenuIds) {
            findMenuDetailBase(mealMenuId, langCode).ifPresent(payload -> result.put(mealMenuId, payload));
        }
        return result;
    }

    void upsertMenuDetailBase(Long mealMenuId, String langCode, MenuDetailBaseCachePayload payload, Duration ttl);

    Optional<MenuDetailRiskDataCachePayload> findMenuDetailRiskData(Long mealMenuId);

    default Map<Long, MenuDetailRiskDataCachePayload> findMenuDetailRiskData(Set<Long> mealMenuIds) {
        Map<Long, MenuDetailRiskDataCachePayload> result = new LinkedHashMap<>();
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return result;
        }
        for (Long mealMenuId : mealMenuIds) {
            findMenuDetailRiskData(mealMenuId).ifPresent(payload -> result.put(mealMenuId, payload));
        }
        return result;
    }

    void upsertMenuDetailRiskData(Long mealMenuId, MenuDetailRiskDataCachePayload payload, Duration ttl);

    Optional<ReligionIngredientMapCachePayload> findReligionIngredientMap();

    void upsertReligionIngredientMap(ReligionIngredientMapCachePayload payload, Duration ttl);
}
