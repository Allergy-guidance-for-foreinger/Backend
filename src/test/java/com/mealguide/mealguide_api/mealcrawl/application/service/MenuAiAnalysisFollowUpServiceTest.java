package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAllergyCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuSpicyLevel;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.PythonMealClientException;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAllergyResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisStatus;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuIngredientResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
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
    void processSplitsTargetsByBatchSize() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(1L, "A");
        persistencePort.menuNames.put(2L, "B");
        persistencePort.menuNames.put(3L, "C");

        FakePythonMealClientPort pythonPort = new FakePythonMealClientPort();
        pythonPort.response = new PythonMenuAnalysisResponse(List.of());

        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setAiAnalysisBatchSize(2);
        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonPort, properties);
        service.process(new MealImportResult(1L, 1L, List.of(1L, 2L, 3L), List.of(1L, 2L, 3L), List.of()));

        assertThat(pythonPort.requests).hasSize(2);
        assertThat(pythonPort.requests.get(0).menus()).hasSize(2);
        assertThat(pythonPort.requests.get(1).menus()).hasSize(1);
    }

    @Test
    void processMapsStatusesAndAttemptCountForMidnightFlow() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(11L, "Menu1");
        persistencePort.menuNames.put(12L, "Menu2");
        persistencePort.menuNames.put(13L, "Menu3");

        FakePythonMealClientPort pythonPort = new FakePythonMealClientPort();
        pythonPort.response = new PythonMenuAnalysisResponse(List.of(
                new PythonMenuAnalysisResultDto(11L, "Menu1", PythonMenuAnalysisStatus.SUCCESS, 1L, null, "m", "v", List.of(
                        new PythonMenuIngredientResultDto("EGG", BigDecimal.ONE)
                ), List.of(new PythonMenuAllergyResultDto("EGG", BigDecimal.ONE))),
                new PythonMenuAnalysisResultDto(12L, "Menu2", PythonMenuAnalysisStatus.RETRYABLE_FAILED, null, "retry", "m", "v", List.of(), List.of()),
                new PythonMenuAnalysisResultDto(13L, "Menu3", PythonMenuAnalysisStatus.PERMANENT_FAILED, null, "bad", "m", "v", List.of(), List.of())
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonPort, new MealCrawlProperties());
        service.process(new MealImportResult(1L, 1L, List.of(11L, 12L, 13L), List.of(11L, 12L, 13L), List.of()));

        assertThat(persistencePort.statusByMenuId.get(11L)).isEqualTo(MenuAiStatus.SUCCESS);
        assertThat(persistencePort.statusByMenuId.get(12L)).isEqualTo(MenuAiStatus.FAILED);
        assertThat(persistencePort.statusByMenuId.get(13L)).isEqualTo(MenuAiStatus.FAILED);
        assertThat(persistencePort.attemptByMenuId.get(11L)).isEqualTo(1);
        assertThat(persistencePort.attemptByMenuId.get(12L)).isEqualTo(1);
        assertThat(persistencePort.attemptByMenuId.get(13L)).isEqualTo(1);
    }

    @Test
    void retryFlowOnlyUsesFailedTargetsAndIncreasesAttemptOnRetryFailure() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.retryTargetMenuIds = List.of(21L);
        persistencePort.menuNames.put(21L, "RetryMenu");

        FakePythonMealClientPort pythonPort = new FakePythonMealClientPort();
        pythonPort.response = new PythonMenuAnalysisResponse(List.of(
                new PythonMenuAnalysisResultDto(21L, "RetryMenu", PythonMenuAnalysisStatus.RETRYABLE_FAILED, null, "still down", "m", "v", List.of(), List.of())
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonPort, new MealCrawlProperties());
        service.processRetryPending("run-1");

        assertThat(persistencePort.lastRetryQueryLimit).isGreaterThan(0);
        assertThat(persistencePort.statusByMenuId.get(21L)).isEqualTo(MenuAiStatus.FAILED);
        assertThat(persistencePort.attemptByMenuId.get(21L)).isEqualTo(2);
    }

    @Test
    void batchClientFailureMarksWholeBatchFailed() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(31L, "A");
        persistencePort.menuNames.put(32L, "B");

        FakePythonMealClientPort pythonPort = new FakePythonMealClientPort();
        pythonPort.exception = new PythonMealClientException("down", 503, "busy", true, null);

        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setAiAnalysisBatchSize(10);
        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonPort, properties);
        service.process(new MealImportResult(1L, 1L, List.of(31L, 32L), List.of(31L, 32L), List.of()));

        assertThat(persistencePort.statusByMenuId.get(31L)).isEqualTo(MenuAiStatus.FAILED);
        assertThat(persistencePort.statusByMenuId.get(32L)).isEqualTo(MenuAiStatus.FAILED);
    }

    private static class FakePythonMealClientPort implements PythonMealClientPort {
        private final List<PythonMenuAnalysisRequest> requests = new ArrayList<>();
        private PythonMenuAnalysisResponse response;
        private RuntimeException exception;

        @Override
        public PythonMealCrawlResponse crawlMeals(PythonMealCrawlRequest request) {
            return null;
        }

        @Override
        public PythonMenuAnalysisResponse analyzeMenus(PythonMenuAnalysisRequest request) {
            requests.add(request);
            if (exception != null) {
                throw exception;
            }
            return response;
        }

        @Override
        public PythonMenuTranslationResponse translateMenus(PythonMenuTranslationRequest request) {
            return null;
        }
    }

    private static class FakeMealCrawlPersistencePort implements MealCrawlPersistencePort {
        private final Map<Long, String> menuNames = new HashMap<>();
        private final Map<Long, MenuAiStatus> statusByMenuId = new HashMap<>();
        private final Map<Long, Integer> attemptByMenuId = new HashMap<>();
        private List<Long> retryTargetMenuIds = List.of();
        private final Map<Long, Integer> latestAttemptsByMenuId = new HashMap<>();
        private int lastRetryQueryLimit;

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
        public List<WeeklyMealCacheRow> findWeeklyMealsForCache(Long cafeteriaId, LocalDate weekStartDate, LocalDate weekEndDate) {
            return List.of();
        }

        @Override
        public boolean existsCafeteriaInSchool(Long cafeteriaId, Long schoolId) {
            return true;
        }

        @Override
        public Map<Long, String> findTranslatedMenuNamesByMealMenuIds(Set<Long> mealMenuIds, String langCode) {
            return Map.of();
        }

        @Override
        public List<MealMenuIngredientRow> findConfirmedIngredientsByMealMenuIds(Set<Long> mealMenuIds) {
            return List.of();
        }

        @Override
        public Set<Long> findMealMenuIdsHavingConfirmedIngredients(Set<Long> mealMenuIds) {
            return Set.of();
        }

        @Override
        public List<MealMenuIngredientRow> findAiIngredientsByMealMenuIds(Set<Long> mealMenuIds) {
            return List.of();
        }

        @Override
        public Set<Long> findMealMenuIdsHavingAiIngredients(Set<Long> mealMenuIds) {
            return Set.of();
        }

        @Override
        public List<RestrictionIngredientRow> findReligiousRestrictionIngredients(List<String> religiousCodes) {
            return List.of();
        }

        @Override
        public Set<Long> findAnalyzedMenuIds(Set<Long> menuIds) {
            return Set.of();
        }

        @Override
        public List<Long> findRetryTargetMenuIds(int limit, int maxAttemptCount) {
            lastRetryQueryLimit = limit;
            return retryTargetMenuIds;
        }

        @Override
        public Map<Long, Integer> findLatestAttemptCounts(Set<Long> menuIds) {
            return latestAttemptsByMenuId;
        }

        @Override
        public Map<Long, String> findMenuNamesByIds(Set<Long> menuIds) {
            return menuNames;
        }

        @Override
        public void saveMenuAnalysis(Long menuId, MenuAiStatus status, String modelName, String modelVersion, String reason, LocalDateTime analyzedAt, int attemptCount, List<MenuIngredientCandidate> ingredients) {
        }

        @Override
        public void updateMenuAiStatus(Long menuId, MenuAiStatus aiStatus, LocalDateTime analyzedAt) {
            statusByMenuId.put(menuId, aiStatus);
        }

        @Override
        public void saveMenuAnalysisAndUpdateStatus(Long menuId, MenuAiStatus status, String modelName, String modelVersion, String reason, LocalDateTime analyzedAt, int attemptCount, List<MenuIngredientCandidate> ingredients, Set<String> validIngredientCodes, List<MenuAllergyCandidate> allergies, Set<String> validAllergyCodes, MenuSpicyLevel spicyLevel) {
            statusByMenuId.put(menuId, status);
            attemptByMenuId.put(menuId, attemptCount);
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
