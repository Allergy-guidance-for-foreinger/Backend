package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MealCrawlOrchestrationServiceTest {

    @Test
    void crawlAndImportKeepsImportSuccessEvenWhenFollowUpsFail() {
        FakePythonClient pythonClient = new FakePythonClient();
        FakePersistencePort persistencePort = new FakePersistencePort();

        MealImportService mealImportService = new MealImportService(persistencePort, new com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties());
        MenuAiAnalysisFollowUpService analysisFollowUpService = new MenuAiAnalysisFollowUpService(persistencePort, pythonClient) {
            @Override
            public void process(MealImportResult importResult) {
                throw new RuntimeException("analysis failure");
            }
        };
        MenuTranslationFollowUpService translationFollowUpService = new MenuTranslationFollowUpService(
                persistencePort,
                pythonClient,
                new com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties()
        ) {
            @Override
            public void process(MealImportResult importResult) {
                throw new RuntimeException("translation failure");
            }
        };

        MealCrawlOrchestrationService orchestrationService = new MealCrawlOrchestrationService(
                pythonClient,
                persistencePort,
                mealImportService,
                mock(WeeklyMealCacheRefreshService.class),
                analysisFollowUpService,
                translationFollowUpService
        );

        MealCrawlTarget target = new MealCrawlTarget(1L, 10L, "School", "Cafe", "http://source", LocalDate.now(), LocalDate.now().plusDays(6));

        assertThatCode(() -> orchestrationService.crawlAndImport(target)).doesNotThrowAnyException();
        assertThat(persistencePort.successMarked).isTrue();
        assertThat(persistencePort.failureMarked).isFalse();
    }

    @Test
    void crawlAndImportKeepsImportSuccessEvenWhenWeeklyCacheRefreshFails() {
        FakePythonClient pythonClient = new FakePythonClient();
        FakePersistencePort persistencePort = new FakePersistencePort();

        MealImportService mealImportService = new MealImportService(persistencePort, new com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties());
        MenuAiAnalysisFollowUpService analysisFollowUpService = new MenuAiAnalysisFollowUpService(persistencePort, pythonClient);
        MenuTranslationFollowUpService translationFollowUpService = new MenuTranslationFollowUpService(
                persistencePort,
                pythonClient,
                new com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties()
        );
        WeeklyMealCacheRefreshService cacheRefreshService = mock(WeeklyMealCacheRefreshService.class);
        doThrow(new RuntimeException("redis failure"))
                .when(cacheRefreshService)
                .refreshWeeklyMealCache(1L, 10L, LocalDate.of(2026, 4, 20));

        MealCrawlOrchestrationService orchestrationService = new MealCrawlOrchestrationService(
                pythonClient,
                persistencePort,
                mealImportService,
                cacheRefreshService,
                analysisFollowUpService,
                translationFollowUpService
        );

        MealCrawlTarget target = new MealCrawlTarget(
                1L,
                10L,
                "School",
                "Cafe",
                "http://source",
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26)
        );

        assertThatCode(() -> orchestrationService.crawlAndImport(target)).doesNotThrowAnyException();
        assertThat(persistencePort.successMarked).isTrue();
        assertThat(persistencePort.failureMarked).isFalse();
        verify(cacheRefreshService).refreshWeeklyMealCache(1L, 10L, LocalDate.of(2026, 4, 20));
    }

    private static class FakePythonClient implements PythonMealClientPort {
        @Override
        public PythonMealCrawlResponse crawlMeals(PythonMealCrawlRequest request) {
            return new PythonMealCrawlResponse(
                    request.schoolName(),
                    request.cafeteriaName(),
                    request.sourceUrl(),
                    request.startDate(),
                    request.endDate(),
                    List.of()
            );
        }

        @Override
        public PythonMenuAnalysisResponse analyzeMenus(PythonMenuAnalysisRequest request) {
            return new PythonMenuAnalysisResponse(List.of());
        }

        @Override
        public PythonMenuTranslationResponse translateMenus(PythonMenuTranslationRequest request) {
            return new PythonMenuTranslationResponse(List.of());
        }
    }

    private static class FakePersistencePort implements MealCrawlPersistencePort {
        private boolean successMarked;
        private boolean failureMarked;

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
            successMarked = true;
        }

        @Override
        public void markCrawlHistoryFailure(Long historyId, String failureMessage, LocalDateTime finishedAt) {
            failureMarked = true;
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
        public List<RestrictionIngredientRow> findAllergyRestrictionIngredients(Set<String> allergyCodes) {
            return List.of();
        }

        @Override
        public List<RestrictionIngredientRow> findReligiousRestrictionIngredients(String religiousCode) {
            return List.of();
        }

        @Override
        public Set<Long> findAnalyzedMenuIds(Set<Long> menuIds) {
            return Set.of();
        }

        @Override
        public Map<Long, String> findMenuNamesByIds(Set<Long> menuIds) {
            return Map.of();
        }

        @Override
        public void saveMenuAnalysis(Long menuId, String status, String modelName, String modelVersion, String reason, LocalDateTime analyzedAt, List<MenuIngredientCandidate> ingredients) {
        }

        @Override
        public void updateMenuAiStatus(Long menuId, String aiStatus, LocalDateTime analyzedAt) {
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

