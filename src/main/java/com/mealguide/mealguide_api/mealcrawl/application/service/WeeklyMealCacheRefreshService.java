package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.WeeklyMealCachePort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyMealCacheRefreshService {

    private static final String MENU_AI_SUCCESS = "SUCCESS";

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final WeeklyMealCachePort weeklyMealCachePort;
    private final MealCrawlProperties mealCrawlProperties;
    private final ObjectMapper objectMapper;

    public void refreshWeeklyMealCache(Long schoolId, Long cafeteriaId, LocalDate weekStartDate) {
        LocalDate normalizedWeekStartDate = WeekStartDateNormalizer.normalize(weekStartDate);
        String redisKey = weeklyMealCachePort.createWeeklyMealCacheKey(schoolId, cafeteriaId, normalizedWeekStartDate);
        try {
            WeeklyMealCachePayload payload = loadWeeklyMealCachePayloadFromDb(schoolId, cafeteriaId, normalizedWeekStartDate);
            int scheduleCount = payload.mealSchedules().size();

            log.info(
                    "Weekly meal cache refresh started: schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}, scheduleCount={}",
                    schoolId, cafeteriaId, normalizedWeekStartDate, redisKey, scheduleCount
            );

            upsertWeeklyMealCachePayload(payload);

            log.info(
                    "Weekly meal cache refresh succeeded: schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}, scheduleCount={}",
                    schoolId, cafeteriaId, normalizedWeekStartDate, redisKey, scheduleCount
            );
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Weekly meal cache refresh failed while serializing payload: schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}",
                    schoolId, cafeteriaId, normalizedWeekStartDate, redisKey, exception
            );
        } catch (Exception exception) {
            log.warn(
                    "Weekly meal cache refresh failed: schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}",
                    schoolId, cafeteriaId, normalizedWeekStartDate, redisKey, exception
            );
        }
    }

    public WeeklyMealCachePayload loadWeeklyMealCachePayloadFromDb(Long schoolId, Long cafeteriaId, LocalDate weekStartDate) {
        LocalDate normalizedWeekStartDate = WeekStartDateNormalizer.normalize(weekStartDate);
        LocalDate weekEndDate = normalizedWeekStartDate.plusDays(6);
        List<WeeklyMealCacheRow> rows = mealCrawlPersistencePort.findWeeklyMealsForCache(cafeteriaId, normalizedWeekStartDate, weekEndDate);
        return buildPayload(schoolId, cafeteriaId, normalizedWeekStartDate, weekEndDate, rows);
    }

    public void upsertWeeklyMealCachePayload(WeeklyMealCachePayload payload) throws JsonProcessingException {
        String serialized = objectMapper.writeValueAsString(payload);
        Duration ttl = Duration.ofSeconds(mealCrawlProperties.getWeeklyMealCacheTtlSeconds());
        weeklyMealCachePort.upsertWeeklyMealCache(
                payload.schoolId(),
                payload.cafeteriaId(),
                payload.weekStartDate(),
                serialized,
                ttl
        );
    }

    private WeeklyMealCachePayload buildPayload(
            Long schoolId,
            Long cafeteriaId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<WeeklyMealCacheRow> rows
    ) {
        Map<MealScheduleKey, List<WeeklyMealCachePayload.MenuItem>> grouped = new LinkedHashMap<>();

        for (WeeklyMealCacheRow row : rows) {
            MealScheduleKey key = new MealScheduleKey(row.mealDate(), row.mealType());
            grouped.computeIfAbsent(key, unused -> new ArrayList<>())
                    .add(new WeeklyMealCachePayload.MenuItem(
                            row.mealMenuId(),
                            row.menuName(),
                            row.cornerName(),
                            row.displayOrder(),
                            row.spicyLevel(),
                            isAiAnalyzed(row.aiAnalysisStatus())
                    ));
        }

        List<WeeklyMealCachePayload.MealScheduleItem> scheduleItems = grouped.entrySet().stream()
                .map(entry -> new WeeklyMealCachePayload.MealScheduleItem(
                        entry.getKey().mealDate(),
                        entry.getKey().mealType(),
                        List.copyOf(entry.getValue())
                ))
                .toList();

        return new WeeklyMealCachePayload(
                schoolId,
                cafeteriaId,
                weekStartDate,
                weekEndDate,
                scheduleItems
        );
    }

    private boolean isAiAnalyzed(String aiAnalysisStatus) {
        if (aiAnalysisStatus == null || aiAnalysisStatus.isBlank()) {
            return false;
        }
        return MENU_AI_SUCCESS.equalsIgnoreCase(aiAnalysisStatus.trim());
    }

    private record MealScheduleKey(LocalDate mealDate, String mealType) {
    }
}
