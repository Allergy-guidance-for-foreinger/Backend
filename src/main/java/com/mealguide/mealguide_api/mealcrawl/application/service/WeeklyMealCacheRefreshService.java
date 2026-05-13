package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.WeeklyMealCachePort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyMealCacheRefreshService {

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final WeeklyMealCachePort weeklyMealCachePort;
    private final MealCrawlProperties mealCrawlProperties;
    private final ObjectMapper objectMapper;

    public void refreshWeeklyMealCache(Long schoolId, Long cafeteriaId, LocalDate weekStartDate) {
        refreshWeeklyMealCache("manual", schoolId, cafeteriaId, weekStartDate);
    }

    public void refreshWeeklyMealCache(String runId, Long schoolId, Long cafeteriaId, LocalDate weekStartDate) {
        Instant startedAt = Instant.now();
        LocalDate normalizedWeekStartDate = WeekStartDateNormalizer.normalize(weekStartDate);
        String redisKey = weeklyMealCachePort.createWeeklyMealCacheKey(schoolId, cafeteriaId, normalizedWeekStartDate);
        try {
            WeeklyMealCachePayload payload = loadWeeklyMealCachePayloadFromDb(schoolId, cafeteriaId, normalizedWeekStartDate);
            int scheduleCount = payload.mealSchedules().size();

            log.info(
                    "event=START stage=cache_refresh runId={} schoolId={} cafeteriaId={} weekStartDate={} redisKey={} scheduleCount={}",
                    runId, schoolId, cafeteriaId, normalizedWeekStartDate, redisKey, scheduleCount
            );

            upsertWeeklyMealCachePayload(payload);
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();

            log.info(
                    "event=END stage=cache_refresh runId={} schoolId={} cafeteriaId={} weekStartDate={} redisKey={} scheduleCount={} durationMs={} result=SUCCESS",
                    runId, schoolId, cafeteriaId, normalizedWeekStartDate, redisKey, scheduleCount, durationMs
            );
        } catch (JsonProcessingException exception) {
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.warn(
                    "event=FAIL stage=cache_refresh runId={} schoolId={} cafeteriaId={} weekStartDate={} redisKey={} durationMs={} errorType=SERIALIZE_FAIL message={}",
                    runId, schoolId, cafeteriaId, normalizedWeekStartDate, redisKey, durationMs, exception.getMessage(), exception
            );
        } catch (Exception exception) {
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.warn(
                    "event=FAIL stage=cache_refresh runId={} schoolId={} cafeteriaId={} weekStartDate={} redisKey={} durationMs={} errorType=CACHE_REFRESH_FAIL message={}",
                    runId, schoolId, cafeteriaId, normalizedWeekStartDate, redisKey, durationMs, exception.getMessage(), exception
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

    private boolean isAiAnalyzed(MenuAiStatus aiAnalysisStatus) {
        return aiAnalysisStatus == MenuAiStatus.SUCCESS;
    }

    private record MealScheduleKey(LocalDate mealDate, String mealType) {
    }
}
