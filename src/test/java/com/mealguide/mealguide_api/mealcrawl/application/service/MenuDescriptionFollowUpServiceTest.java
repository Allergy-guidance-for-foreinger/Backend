package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuDescriptionKey;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuDescriptionStatus;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuDescriptionRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuDescriptionResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuDescriptionResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuDescriptionFollowUpServiceTest {

    @Test
    void processCallsPythonInBatchesOfSeven() {
        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setDescriptionTargetLanguages(List.of("en"));
        properties.setDescriptionBatchSize(7);

        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        PythonMealClientPort pythonClientPort = mock(PythonMealClientPort.class);
        Set<Long> menuIds = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        when(persistencePort.findExistingMenuDescriptionKeys(menuIds, List.of("en"))).thenReturn(Set.of());
        when(persistencePort.findMenuNamesByIds(menuIds)).thenReturn(menuNames(8));
        when(pythonClientPort.describeMenus(any())).thenAnswer(invocation -> {
            PythonMenuDescriptionRequest request = invocation.getArgument(0);
            return new PythonMenuDescriptionResponse(request.menus().stream()
                    .map(target -> new PythonMenuDescriptionResultDto(target.menuId(), "description-" + target.menuId()))
                    .toList());
        });

        MenuDescriptionFollowUpService service = new MenuDescriptionFollowUpService(persistencePort, pythonClientPort, properties);
        service.process(new MealImportResult(1L, 2L, List.copyOf(menuIds), List.of(), List.of()));

        ArgumentCaptor<PythonMenuDescriptionRequest> requestCaptor = ArgumentCaptor.forClass(PythonMenuDescriptionRequest.class);
        verify(pythonClientPort, times(2)).describeMenus(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).extracting(request -> request.menus().size()).containsExactly(7, 1);
        verify(persistencePort).saveMenuDescriptions(org.mockito.ArgumentMatchers.<Map<MenuDescriptionKey, String>>argThat(saved ->
                saved.size() == 8 && saved.containsKey(new MenuDescriptionKey(1L, "en"))
        ));
    }

    @Test
    void processMarksOnlyInvalidMenuDescriptionsAsFailed() {
        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setDescriptionTargetLanguages(List.of("en"));

        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        PythonMealClientPort pythonClientPort = mock(PythonMealClientPort.class);
        Set<Long> menuIds = Set.of(1L, 2L, 3L);
        when(persistencePort.findExistingMenuDescriptionKeys(menuIds, List.of("en"))).thenReturn(Set.of());
        when(persistencePort.findMenuNamesByIds(menuIds)).thenReturn(menuNames(3));
        when(pythonClientPort.describeMenus(any())).thenReturn(new PythonMenuDescriptionResponse(List.of(
                new PythonMenuDescriptionResultDto(1L, "valid description"),
                new PythonMenuDescriptionResultDto(2L, " "),
                new PythonMenuDescriptionResultDto(3L, "x".repeat(301))
        )));

        MenuDescriptionFollowUpService service = new MenuDescriptionFollowUpService(persistencePort, pythonClientPort, properties);
        service.process(new MealImportResult(1L, 2L, List.copyOf(menuIds), List.of(), List.of()));

        verify(persistencePort).saveMenuDescriptions(org.mockito.ArgumentMatchers.<Map<MenuDescriptionKey, String>>argThat(saved ->
                saved.size() == 1 && "valid description".equals(saved.get(new MenuDescriptionKey(1L, "en")))
        ));
        verify(persistencePort).saveMenuDescriptionAnalysis(2L, "en", MenuDescriptionStatus.FAILED, "Blank description", 1);
        verify(persistencePort).saveMenuDescriptionAnalysis(3L, "en", MenuDescriptionStatus.FAILED, "Description exceeds 300 characters", 1);
        verify(persistencePort, atLeastOnce()).saveMenuDescriptionAnalysis(1L, "en", MenuDescriptionStatus.SUCCESS, null, 1);
    }

    private Map<Long, String> menuNames(int count) {
        Map<Long, String> menuNames = new LinkedHashMap<>();
        for (long id = 1; id <= count; id++) {
            menuNames.put(id, "Menu-" + id);
        }
        return menuNames;
    }
}
