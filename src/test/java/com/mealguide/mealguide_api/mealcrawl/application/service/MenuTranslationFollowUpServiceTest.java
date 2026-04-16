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
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonTranslatedMenuNameDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MenuTranslationFollowUpServiceTest {

    @Test
    void processRequestsOnlyMenusMissingTranslations() {
        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setTranslationTargetLanguages(List.of("en", "ja"));

        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(11L, "Bibimbap");
        persistencePort.menuNames.put(12L, "Kimchi");
        persistencePort.existingTranslations.add(new MenuTranslationKey(11L, "en"));
        persistencePort.existingTranslations.add(new MenuTranslationKey(11L, "ja"));

        FakePythonMealClientPort pythonClientPort = new FakePythonMealClientPort();
        pythonClientPort.translationResponse = new PythonMenuTranslationResponse(List.of(
                new PythonMenuTranslationResultDto(
                        12L,
                        "Kimchi",
                        List.of(new PythonTranslatedMenuNameDto("en", "Kimchi"), new PythonTranslatedMenuNameDto("ja", "Kimuchee"))
                )
        ));

        MenuTranslationFollowUpService service = new MenuTranslationFollowUpService(persistencePort, pythonClientPort, properties);
        service.process(new MealImportResult(1L, 2L, List.of(11L, 12L), List.of(), List.of(11L, 12L)));

        assertThat(pythonClientPort.lastTranslationRequest.menus())
                .extracting(PythonMenuTranslationTargetDto::menuId)
                .containsExactly(12L);
        assertThat(persistencePort.savedTranslationKeys)
                .containsExactlyInAnyOrder(new MenuTranslationKey(12L, "en"), new MenuTranslationKey(12L, "ja"));
    }

    private static class FakePythonMealClientPort implements PythonMealClientPort {
        private PythonMenuTranslationRequest lastTranslationRequest;
        private PythonMenuTranslationResponse translationResponse;

        @Override
        public PythonMealCrawlResponse crawlMeals(PythonMealCrawlRequest request) {
            return null;
        }

        @Override
        public PythonMenuAnalysisResponse analyzeMenus(PythonMenuAnalysisRequest request) {
            return null;
        }

        @Override
        public PythonMenuTranslationResponse translateMenus(PythonMenuTranslationRequest request) {
            this.lastTranslationRequest = request;
            return translationResponse;
        }
    }

    private static class FakeMealCrawlPersistencePort implements MealCrawlPersistencePort {
        private final Map<Long, String> menuNames = new HashMap<>();
        private final Set<MenuTranslationKey> existingTranslations = new HashSet<>();
        private final Set<MenuTranslationKey> savedTranslationKeys = new HashSet<>();

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
        }

        @Override
        public void updateMenuAiStatus(Long menuId, String aiStatus, LocalDateTime analyzedAt) {
        }

        @Override
        public Set<MenuTranslationKey> findExistingMenuTranslationKeys(Set<Long> menuIds, List<String> langCodes) {
            return new HashSet<>(existingTranslations);
        }

        @Override
        public void saveMenuTranslation(Long menuId, String langCode, String translatedName) {
            savedTranslationKeys.add(new MenuTranslationKey(menuId, langCode));
        }
    }
}

