package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.WeeklyMealCachePort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuRiskLevel;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.WeeklyMealResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyMealQueryService {

    private final MealUserPreferencePort mealUserPreferencePort;
    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final WeeklyMealCachePort weeklyMealCachePort;
    private final WeeklyMealCacheRefreshService weeklyMealCacheRefreshService;
    private final WeeklyMealResponseAssembler weeklyMealResponseAssembler;
    private final ObjectMapper objectMapper;

    public WeeklyMealResponse getWeeklyMeals(Long userId, Long cafeteriaId, LocalDate weekStartDate) {
        LocalDate normalizedWeekStartDate = WeekStartDateNormalizer.normalize(weekStartDate);
        CurrentUserMealPreference preference = mealUserPreferencePort.getCurrentUserMealPreference(userId);
        Long schoolId = requireSchoolId(preference);
        validateCafeteriaBelongsToSchool(cafeteriaId, schoolId);

        String redisKey = weeklyMealCachePort.createWeeklyMealCacheKey(schoolId, cafeteriaId, normalizedWeekStartDate);
        WeeklyMealCachePayload payload = loadPayloadFromCache(userId, schoolId, cafeteriaId, normalizedWeekStartDate, redisKey)
                .orElseGet(() -> loadPayloadFromDbFallback(userId, schoolId, cafeteriaId, normalizedWeekStartDate, redisKey));
        Map<Long, String> translatedMenuNames = resolveTranslatedMenuNames(payload, preference.languageCode());

        log.info(
                "Weekly meal risk evaluation started: userId={}, schoolId={}, cafeteriaId={}, weekStartDate={}, menuCount={}",
                userId,
                schoolId,
                cafeteriaId,
                normalizedWeekStartDate,
                countMenus(payload)
        );

        try {
            WeeklyMealResponse response = weeklyMealResponseAssembler.assemble(payload, preference, translatedMenuNames);
            log.info(
                    "Weekly meal risk evaluation completed: userId={}, schoolId={}, cafeteriaId={}, weekStartDate={}, menuCount={}",
                    userId,
                    schoolId,
                    cafeteriaId,
                    normalizedWeekStartDate,
                    countMenus(payload)
            );
            return response;
        } catch (Exception exception) {
            log.warn(
                    "Weekly meal risk evaluation failed. Returning UNKNOWN for all menus: userId={}, schoolId={}, cafeteriaId={}, weekStartDate={}",
                    userId, schoolId, cafeteriaId, normalizedWeekStartDate, exception
            );
            return toResponseWithUnknownRisk(payload, translatedMenuNames);
        }
    }

    private Optional<WeeklyMealCachePayload> loadPayloadFromCache(
            Long userId,
            Long schoolId,
            Long cafeteriaId,
            LocalDate weekStartDate,
            String redisKey
    ) {
        try {
            Optional<String> cached = weeklyMealCachePort.findWeeklyMealCache(schoolId, cafeteriaId, weekStartDate);
            if (cached.isEmpty()) {
                log.info(
                        "Weekly meal cache miss: userId={}, schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}",
                        userId, schoolId, cafeteriaId, weekStartDate, redisKey
                );
                return Optional.empty();
            }

            WeeklyMealCachePayload payload = objectMapper.readValue(cached.get(), WeeklyMealCachePayload.class);
            log.info(
                    "Weekly meal cache hit: userId={}, schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}",
                    userId, schoolId, cafeteriaId, weekStartDate, redisKey
            );
            return Optional.of(payload);
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Weekly meal cache deserialization failed: userId={}, schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}",
                    userId, schoolId, cafeteriaId, weekStartDate, redisKey, exception
            );
            return Optional.empty();
        } catch (Exception exception) {
            log.warn(
                    "Weekly meal cache read failed. DB fallback will be used: userId={}, schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}",
                    userId, schoolId, cafeteriaId, weekStartDate, redisKey, exception
            );
            return Optional.empty();
        }
    }

    private WeeklyMealCachePayload loadPayloadFromDbFallback(
            Long userId,
            Long schoolId,
            Long cafeteriaId,
            LocalDate weekStartDate,
            String redisKey
    ) {
        log.info(
                "Weekly meal DB fallback started: userId={}, schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}",
                userId, schoolId, cafeteriaId, weekStartDate, redisKey
        );
        WeeklyMealCachePayload payload = weeklyMealCacheRefreshService.loadWeeklyMealCachePayloadFromDb(schoolId, cafeteriaId, weekStartDate);
        try {
            weeklyMealCacheRefreshService.upsertWeeklyMealCachePayload(payload);
        } catch (Exception exception) {
            log.warn(
                    "Weekly meal cache write failed during DB fallback: userId={}, schoolId={}, cafeteriaId={}, weekStartDate={}, redisKey={}",
                    userId, schoolId, cafeteriaId, weekStartDate, redisKey, exception
            );
        }
        return payload;
    }

    private void validateCafeteriaBelongsToSchool(Long cafeteriaId, Long schoolId) {
        if (!mealCrawlPersistencePort.existsCafeteriaInSchool(cafeteriaId, schoolId)) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
    }

    private Long requireSchoolId(CurrentUserMealPreference preference) {
        if (preference.schoolId() == null) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        return preference.schoolId();
    }

    private int countMenus(WeeklyMealCachePayload payload) {
        int count = 0;
        for (WeeklyMealCachePayload.MealScheduleItem schedule : payload.mealSchedules()) {
            count += schedule.menus().size();
        }
        return count;
    }

    private WeeklyMealResponse toResponseWithUnknownRisk(
            WeeklyMealCachePayload payload,
            Map<Long, String> translatedMenuNamesByMealMenuId
    ) {
        return new WeeklyMealResponse(
                payload.schoolId(),
                payload.cafeteriaId(),
                payload.weekStartDate(),
                payload.weekEndDate(),
                payload.mealSchedules().stream()
                        .map(schedule -> new WeeklyMealResponse.MealScheduleResponse(
                                schedule.mealDate(),
                                schedule.mealType(),
                                schedule.menus().stream()
                                        .map(menu -> new WeeklyMealResponse.MenuResponse(
                                                menu.mealMenuId(),
                                                translatedMenuNamesByMealMenuId.getOrDefault(menu.mealMenuId(), menu.menuName()),
                                                menu.cornerName(),
                                                menu.displayOrder(),
                                                menu.spicyLevel(),
                                                menu.aiAnalyzed(),
                                                new WeeklyMealResponse.MenuRiskResponse(MenuRiskLevel.UNKNOWN.name(), List.of())
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    private Map<Long, String> resolveTranslatedMenuNames(WeeklyMealCachePayload payload, String languageCode) {
        try {
            return weeklyMealResponseAssembler.resolveTranslatedMenuNames(payload, languageCode);
        } catch (Exception exception) {
            log.warn("Weekly meal translated menu name loading failed. Default menu names will be used.", exception);
            return Map.of();
        }
    }
}
