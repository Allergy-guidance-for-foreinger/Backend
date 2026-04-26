package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.WeeklyMealCachePort;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.WeeklyMealResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeeklyMealQueryServiceTest {

    @Test
    void cacheHitUsesCachedPayload() throws Exception {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        WeeklyMealCachePort cachePort = mock(WeeklyMealCachePort.class);
        WeeklyMealCacheRefreshService cacheRefreshService = mock(WeeklyMealCacheRefreshService.class);
        WeeklyMealResponseAssembler assembler = mock(WeeklyMealResponseAssembler.class);

        when(preferencePort.getCurrentUserMealPreference(1L)).thenReturn(samplePreference());
        when(persistencePort.existsCafeteriaInSchool(10L, 100L)).thenReturn(true);
        when(cachePort.createWeeklyMealCacheKey(100L, 10L, LocalDate.of(2026, 4, 20)))
                .thenReturn("meal:weekly:100:10:2026-04-20");

        WeeklyMealCachePayload payload = samplePayload();
        String serialized = createObjectMapper().writeValueAsString(payload);
        when(cachePort.findWeeklyMealCache(100L, 10L, LocalDate.of(2026, 4, 20)))
                .thenReturn(Optional.of(serialized));
        when(assembler.assemble(payload, samplePreference()))
                .thenReturn(sampleResponse());

        WeeklyMealQueryService service = new WeeklyMealQueryService(
                preferencePort,
                persistencePort,
                cachePort,
                cacheRefreshService,
                assembler,
                createObjectMapper()
        );

        WeeklyMealResponse response = service.getWeeklyMeals(1L, 10L, LocalDate.of(2026, 4, 20));

        assertThat(response.mealSchedules().get(0).menus().get(0).mealMenuId()).isEqualTo(11L);
        verify(cacheRefreshService, never()).loadWeeklyMealCachePayloadFromDb(any(), any(), any());
    }

    @Test
    void cacheMissUsesDbFallbackAndUpsert() throws Exception {
        MealUserPreferencePort preferencePort = mock(MealUserPreferencePort.class);
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        WeeklyMealCachePort cachePort = mock(WeeklyMealCachePort.class);
        WeeklyMealCacheRefreshService cacheRefreshService = mock(WeeklyMealCacheRefreshService.class);
        WeeklyMealResponseAssembler assembler = mock(WeeklyMealResponseAssembler.class);

        when(preferencePort.getCurrentUserMealPreference(1L)).thenReturn(samplePreference());
        when(persistencePort.existsCafeteriaInSchool(10L, 100L)).thenReturn(true);
        when(cachePort.createWeeklyMealCacheKey(100L, 10L, LocalDate.of(2026, 4, 20)))
                .thenReturn("meal:weekly:100:10:2026-04-20");
        when(cachePort.findWeeklyMealCache(100L, 10L, LocalDate.of(2026, 4, 20)))
                .thenReturn(Optional.empty());

        WeeklyMealCachePayload payload = samplePayload();
        when(cacheRefreshService.loadWeeklyMealCachePayloadFromDb(100L, 10L, LocalDate.of(2026, 4, 20)))
                .thenReturn(payload);
        when(assembler.assemble(payload, samplePreference()))
                .thenReturn(sampleResponse());

        WeeklyMealQueryService service = new WeeklyMealQueryService(
                preferencePort,
                persistencePort,
                cachePort,
                cacheRefreshService,
                assembler,
                createObjectMapper()
        );

        WeeklyMealResponse response = service.getWeeklyMeals(1L, 10L, LocalDate.of(2026, 4, 20));

        assertThat(response.mealSchedules().get(0).menus().get(0).mealMenuId()).isEqualTo(11L);
        verify(cacheRefreshService).loadWeeklyMealCachePayloadFromDb(100L, 10L, LocalDate.of(2026, 4, 20));
        verify(cacheRefreshService).upsertWeeklyMealCachePayload(payload);
    }

    private CurrentUserMealPreference samplePreference() {
        return new CurrentUserMealPreference(1L, 100L, "en", "HALAL", List.of("PORK"));
    }

    private WeeklyMealCachePayload samplePayload() {
        return new WeeklyMealCachePayload(
                100L,
                10L,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                List.of(new WeeklyMealCachePayload.MealScheduleItem(
                        LocalDate.of(2026, 4, 20),
                        "LUNCH",
                        List.of(new WeeklyMealCachePayload.MenuItem(
                                11L,
                                "Kimchi Stew",
                                "Korean",
                                1,
                                2L,
                                true
                        ))
                ))
        );
    }

    private WeeklyMealResponse sampleResponse() {
        return new WeeklyMealResponse(
                100L,
                10L,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                List.of(new WeeklyMealResponse.MealScheduleResponse(
                        LocalDate.of(2026, 4, 20),
                        "LUNCH",
                        List.of(new WeeklyMealResponse.MenuResponse(
                                11L,
                                "Kimchi Stew",
                                "Korean",
                                1,
                                2L,
                                true,
                                new WeeklyMealResponse.MenuRiskResponse("SAFE", List.of())
                        ))
                ))
        );
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
