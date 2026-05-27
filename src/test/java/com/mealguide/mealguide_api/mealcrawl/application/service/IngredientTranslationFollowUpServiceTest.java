package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.IngredientTranslationTarget;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonIngredientTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonIngredientTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonIngredientTranslationResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngredientTranslationFollowUpServiceTest {

    @Test
    void processRunsMultipleSmallBatchesAndSavesSuccessfulTranslations() {
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        PythonMealClientPort pythonPort = mock(PythonMealClientPort.class);
        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setIngredientTranslationBatchSize(2);
        properties.setIngredientTranslationMaxBatchesPerRun(2);

        when(persistencePort.findMissingIngredientTranslationTargets(eq("ko"), eq("en"), eq(2), anySet()))
                .thenReturn(
                        List.of(
                                new IngredientTranslationTarget("AI_1", "베이컨"),
                                new IngredientTranslationTarget("AI_2", "소고기 패티")
                        ),
                        List.of(new IngredientTranslationTarget("AI_3", "체다 치즈"))
                );
        when(pythonPort.translateIngredients(any(PythonIngredientTranslationRequest.class)))
                .thenReturn(
                        new PythonIngredientTranslationResponse(List.of(
                                new PythonIngredientTranslationResultDto("AI_1", "Bacon"),
                                new PythonIngredientTranslationResultDto("AI_2", "Beef patty")
                        )),
                        new PythonIngredientTranslationResponse(List.of(
                                new PythonIngredientTranslationResultDto("AI_3", "Cheddar cheese")
                        ))
                );

        IngredientTranslationFollowUpService service = new IngredientTranslationFollowUpService(
                persistencePort,
                pythonPort,
                properties
        );
        service.process("run-1");

        ArgumentCaptor<PythonIngredientTranslationRequest> requestCaptor =
                ArgumentCaptor.forClass(PythonIngredientTranslationRequest.class);
        verify(pythonPort, org.mockito.Mockito.times(2)).translateIngredients(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).ingredients()).hasSize(2);
        assertThat(requestCaptor.getAllValues().get(1).ingredients()).hasSize(1);

        ArgumentCaptor<Map<String, String>> saveCaptor = ArgumentCaptor.forClass(Map.class);
        verify(persistencePort, org.mockito.Mockito.times(2)).saveIngredientTranslations(eq("en"), saveCaptor.capture());
        assertThat(saveCaptor.getAllValues().get(0))
                .containsEntry("AI_1", "Bacon")
                .containsEntry("AI_2", "Beef patty");
        assertThat(saveCaptor.getAllValues().get(1))
                .containsEntry("AI_3", "Cheddar cheese");
    }
}
