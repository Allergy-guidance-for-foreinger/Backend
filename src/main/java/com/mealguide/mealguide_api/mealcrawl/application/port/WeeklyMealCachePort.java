package com.mealguide.mealguide_api.mealcrawl.application.port;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyMealCachePort {

    String createWeeklyMealCacheKey(Long schoolId, Long cafeteriaId, LocalDate weekStartDate);

    Optional<String> findWeeklyMealCache(Long schoolId, Long cafeteriaId, LocalDate weekStartDate);

    void upsertWeeklyMealCache(Long schoolId, Long cafeteriaId, LocalDate weekStartDate, String payload, Duration ttl);
}
