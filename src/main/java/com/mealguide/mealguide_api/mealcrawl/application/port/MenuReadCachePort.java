package com.mealguide.mealguide_api.mealcrawl.application.port;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailBaseCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRiskDataCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.ReligionIngredientMapCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealI18nCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealRiskDataCachePayload;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

public interface MenuReadCachePort {

    Optional<WeeklyMealRiskDataCachePayload> findWeeklyMealRiskData(Long cafeteriaId, LocalDate weekStartDate);

    void upsertWeeklyMealRiskData(Long cafeteriaId, LocalDate weekStartDate, WeeklyMealRiskDataCachePayload payload, Duration ttl);

    Optional<WeeklyMealI18nCachePayload> findWeeklyMealI18n(Long cafeteriaId, LocalDate weekStartDate, String langCode);

    void upsertWeeklyMealI18n(Long cafeteriaId, LocalDate weekStartDate, String langCode, WeeklyMealI18nCachePayload payload, Duration ttl);

    Optional<MenuDetailBaseCachePayload> findMenuDetailBase(Long mealMenuId, String langCode);

    void upsertMenuDetailBase(Long mealMenuId, String langCode, MenuDetailBaseCachePayload payload, Duration ttl);

    Optional<MenuDetailRiskDataCachePayload> findMenuDetailRiskData(Long mealMenuId);

    void upsertMenuDetailRiskData(Long mealMenuId, MenuDetailRiskDataCachePayload payload, Duration ttl);

    Optional<ReligionIngredientMapCachePayload> findReligionIngredientMap();

    void upsertReligionIngredientMap(ReligionIngredientMapCachePayload payload, Duration ttl);
}
