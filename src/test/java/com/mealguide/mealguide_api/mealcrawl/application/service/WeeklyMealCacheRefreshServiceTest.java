package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.WeeklyMealCachePort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeeklyMealCacheRefreshServiceTest {

    @Test
    void refreshWeeklyMealCacheForSchoolStoresWeeklyMealsAsJson() throws Exception {
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        WeeklyMealCachePort weeklyMealCachePort = mock(WeeklyMealCachePort.class);
        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setWeeklyMealCacheTtlSeconds(86400L);

        LocalDate weekStartDate = LocalDate.of(2026, 4, 20);
        when(weeklyMealCachePort.createWeeklyMealCacheKey(1L, 10L, weekStartDate))
                .thenReturn("meal:weekly:1:10:2026-04-20");
        when(persistencePort.findWeeklyMealsForCache(10L, weekStartDate, weekStartDate.plusDays(6)))
                .thenReturn(List.of(
                        new WeeklyMealCacheRow(LocalDate.of(2026, 4, 20), "LUNCH", 1, "A", 11L, "Kimchi Stew", 2L, "SUCCESS"),
                        new WeeklyMealCacheRow(LocalDate.of(2026, 4, 20), "LUNCH", 2, "B", 12L, "Rice", 0L, "PENDING"),
                        new WeeklyMealCacheRow(LocalDate.of(2026, 4, 20), "LUNCH", 3, "C", 13L, "Fish", 0L, "FAILURE")
                ));

        WeeklyMealCacheRefreshService service = new WeeklyMealCacheRefreshService(
                persistencePort,
                weeklyMealCachePort,
                properties,
                createObjectMapper()
        );

        service.refreshWeeklyMealCache(1L, 10L, weekStartDate);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(weeklyMealCachePort).upsertWeeklyMealCache(
                eq(1L),
                eq(10L),
                eq(weekStartDate),
                payloadCaptor.capture(),
                eq(Duration.ofSeconds(86400L))
        );

        JsonNode root = createObjectMapper().readTree(payloadCaptor.getValue());
        assertThat(root.get("schoolId").asLong()).isEqualTo(1L);
        assertThat(root.get("cafeteriaId").asLong()).isEqualTo(10L);
        assertThat(root.get("weekStartDate").asText()).isEqualTo("2026-04-20");
        assertThat(root.get("mealSchedules").get(0).get("menus")).hasSize(3);
        assertThat(root.get("mealSchedules").get(0).get("menus").get(0).get("mealMenuId").asLong()).isEqualTo(11L);
        assertThat(root.get("mealSchedules").get(0).get("menus").get(0).get("spicyLevel").asLong()).isEqualTo(2L);
        assertThat(root.get("mealSchedules").get(0).get("menus").get(0).get("aiAnalyzed").asBoolean()).isTrue();
        assertThat(root.get("mealSchedules").get(0).get("menus").get(1).get("aiAnalyzed").asBoolean()).isFalse();
        assertThat(root.get("mealSchedules").get(0).get("menus").get(2).get("aiAnalyzed").asBoolean()).isFalse();
    }

    @Test
    void refreshWeeklyMealCacheForSchoolDoesNotThrowWhenRedisSaveFails() {
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        WeeklyMealCachePort weeklyMealCachePort = mock(WeeklyMealCachePort.class);
        MealCrawlProperties properties = new MealCrawlProperties();

        LocalDate weekStartDate = LocalDate.of(2026, 4, 20);
        when(weeklyMealCachePort.createWeeklyMealCacheKey(1L, 10L, weekStartDate))
                .thenReturn("meal:weekly:1:10:2026-04-20");
        when(persistencePort.findWeeklyMealsForCache(10L, weekStartDate, weekStartDate.plusDays(6)))
                .thenReturn(List.of());
        doThrow(new RuntimeException("redis down"))
                .when(weeklyMealCachePort)
                .upsertWeeklyMealCache(eq(1L), eq(10L), eq(weekStartDate), any(String.class), any(Duration.class));

        WeeklyMealCacheRefreshService service = new WeeklyMealCacheRefreshService(
                persistencePort,
                weeklyMealCachePort,
                properties,
                createObjectMapper()
        );

        assertThatCode(() -> service.refreshWeeklyMealCache(1L, 10L, weekStartDate)).doesNotThrowAnyException();
    }

    @Test
    void refreshWeeklyMealCacheNormalizesWeekStartDateToMonday() {
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        WeeklyMealCachePort weeklyMealCachePort = mock(WeeklyMealCachePort.class);
        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setWeeklyMealCacheTtlSeconds(86400L);

        LocalDate requestedDate = LocalDate.of(2026, 4, 22);
        LocalDate normalizedMonday = LocalDate.of(2026, 4, 20);
        when(weeklyMealCachePort.createWeeklyMealCacheKey(1L, 10L, normalizedMonday))
                .thenReturn("meal:weekly:1:10:2026-04-20");
        when(persistencePort.findWeeklyMealsForCache(10L, normalizedMonday, normalizedMonday.plusDays(6)))
                .thenReturn(List.of());

        WeeklyMealCacheRefreshService service = new WeeklyMealCacheRefreshService(
                persistencePort,
                weeklyMealCachePort,
                properties,
                createObjectMapper()
        );

        service.refreshWeeklyMealCache(1L, 10L, requestedDate);

        verify(weeklyMealCachePort).createWeeklyMealCacheKey(1L, 10L, normalizedMonday);
        verify(weeklyMealCachePort, never()).createWeeklyMealCacheKey(1L, 10L, requestedDate);
        verify(persistencePort).findWeeklyMealsForCache(10L, normalizedMonday, normalizedMonday.plusDays(6));
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
