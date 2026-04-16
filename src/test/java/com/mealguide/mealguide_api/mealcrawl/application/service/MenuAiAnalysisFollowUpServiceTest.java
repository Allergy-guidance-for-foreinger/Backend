package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuIngredientResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MenuAiAnalysisFollowUpServiceTest {

    @Test
    void processRequestsOnlyMenusNeedingAnalysis() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(11L, "Bibimbap");
        persistencePort.menuNames.put(12L, "Kimchi");

        FakePythonMealClientPort pythonClientPort = new FakePythonMealClientPort();
        pythonClientPort.analysisResponse = new PythonMenuAnalysisResponse(List.of(
                new PythonMenuAnalysisResultDto(
                        11L,
                        "Bibimbap",
                        "SUCCESS",
                        null,
                        "gpt",
                        "1",
                        LocalDateTime.now(),
                        List.of(new PythonMenuIngredientResultDto("ING_A", BigDecimal.valueOf(0.91)))
                )
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonClientPort);
        MealImportResult importResult = new MealImportResult(1L, 2L, List.of(11L, 12L), List.of(11L, 12L), List.of());

        service.process(importResult);

        assertThat(pythonClientPort.lastAnalysisRequest.menus())
                .extracting(PythonMenuAnalysisTargetDto::menuId)
                .containsExactlyInAnyOrder(11L, 12L);
        assertThat(persistencePort.savedAnalysisMenuIds).containsExactlyInAnyOrder(11L, 12L);
        assertThat(persistencePort.updatedMenuStatus.get(11L)).isEqualTo("SUCCESS");
        assertThat(persistencePort.updatedMenuStatus.get(12L)).isEqualTo("FAILED");
    }

    private static class FakePythonMealClientPort implements PythonMealClientPort {
        private PythonMenuAnalysisRequest lastAnalysisRequest;
        private PythonMenuAnalysisResponse analysisResponse;

        @Override
        public PythonMealCrawlResponse crawlMeals(PythonMealCrawlRequest request) {
            return null;
        }

        @Override
        public PythonMenuAnalysisResponse analyzeMenus(PythonMenuAnalysisRequest request) {
            this.lastAnalysisRequest = request;
            return analysisResponse;
        }

        @Override
        public PythonMenuTranslationResponse translateMenus(PythonMenuTranslationRequest request) {
            return null;
        }
    }

    private static class FakeMealCrawlPersistencePort implements MealCrawlPersistencePort {
        private final Map<Long, String> menuNames = new HashMap<>();
        private final List<Long> savedAnalysisMenuIds = new ArrayList<>();
        private final Map<Long, String> updatedMenuStatus = new HashMap<>();

        @Override
        public List<CrawlTargetSource> findCrawlTargets() {
            return List.of();
        }

        @Override
        public Long startCrawlHistory(Long cafeteriaId, LocalDate startDate, LocalDate endDate, LocalDateTime startedAt) {
            return 1L;
        }

        @Override
        public void markCrawlHistorySuccess(Long historyId, LocalDateTime finishedAt) {
        }

        @Override
        public void markCrawlHistoryFailure(Long historyId, String failureMessage, LocalDateTime finishedAt) {
        }

        @Override
        public Long getOrCreateMealSchedule(Long cafeteriaId, LocalDate mealDate, String mealType) {
            return 1L;
        }

        @Override
        public Long getOrCreateMenu(String menuName) {
            return 1L;
        }

        @Override
        public void upsertMealMenu(Long mealScheduleId, Long menuId, String cornerName, int displayOrder) {
        }

        @Override
        public Set<Long> findAnalyzedMenuIds(Set<Long> menuIds) {
            return Set.of();
        }

        @Override
        public Map<Long, String> findMenuNamesByIds(Set<Long> menuIds) {
            return menuNames;
        }

        @Override
        public void saveMenuAnalysis(Long menuId, String status, String modelName, String modelVersion, String reason, LocalDateTime analyzedAt, List<MenuIngredientCandidate> ingredients) {
            savedAnalysisMenuIds.add(menuId);
        }

        @Override
        public void updateMenuAiStatus(Long menuId, String aiStatus, LocalDateTime analyzedAt) {
            updatedMenuStatus.put(menuId, aiStatus);
        }

        @Override
        public Set<MenuTranslationKey> findExistingMenuTranslationKeys(Set<Long> menuIds, List<String> langCodes) {
            return Set.of();
        }

        @Override
        public void saveMenuTranslation(Long menuId, String langCode, String translatedName) {
        }
    }
}

