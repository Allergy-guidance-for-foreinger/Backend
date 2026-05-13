package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuSpicyLevel;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAllergyResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisStatus;
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
                        PythonMenuAnalysisStatus.SUCCESS,
                        null,
                        "gpt",
                        "1",
                        List.of(new PythonMenuIngredientResultDto("ING_A", BigDecimal.valueOf(0.91)))
                )
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonClientPort);
        MealImportResult importResult = new MealImportResult(1L, 2L, List.of(11L, 12L), List.of(11L, 12L), List.of());

        service.process(importResult);

        assertThat(pythonClientPort.lastAnalysisRequest.menus())
                .extracting(PythonMenuAnalysisTargetDto::menuId)
                .containsExactlyInAnyOrder(11L, 12L);
        assertThat(pythonClientPort.lastAnalysisRequest.includeIngredients()).isTrue();
        assertThat(pythonClientPort.lastAnalysisRequest.includeAllergies()).isTrue();
        assertThat(persistencePort.savedAnalysisMenuIds).containsExactlyInAnyOrder(11L, 12L);
        assertThat(persistencePort.updatedMenuStatus.get(11L)).isEqualTo(MenuAiStatus.SUCCESS);
        assertThat(persistencePort.updatedMenuStatus.get(12L)).isEqualTo(MenuAiStatus.FAILED);
    }

    @Test
    void processTreatsCompletedLikeStatusAsSuccess() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(21L, "Udon");

        FakePythonMealClientPort pythonClientPort = new FakePythonMealClientPort();
        pythonClientPort.analysisResponse = new PythonMenuAnalysisResponse(List.of(
                new PythonMenuAnalysisResultDto(
                        21L,
                        "Udon",
                        PythonMenuAnalysisStatus.SUCCESS,
                        4L,
                        null,
                        "gpt",
                        "1",
                        List.of(new PythonMenuIngredientResultDto("WHEAT", BigDecimal.valueOf(0.95)))
                )
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonClientPort);
        MealImportResult importResult = new MealImportResult(1L, 1L, List.of(21L), List.of(21L), List.of());

        service.process(importResult);

        assertThat(persistencePort.updatedMenuStatus.get(21L)).isEqualTo(MenuAiStatus.SUCCESS);
        assertThat(persistencePort.updatedSpicyLevel.get(21L)).isEqualTo(MenuSpicyLevel.LEVEL_4);
    }

    @Test
    void processAcceptsSpicyLevelZero() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(22L, "Salad");

        FakePythonMealClientPort pythonClientPort = new FakePythonMealClientPort();
        pythonClientPort.analysisResponse = new PythonMenuAnalysisResponse(List.of(
                new PythonMenuAnalysisResultDto(
                        22L,
                        "Salad",
                        PythonMenuAnalysisStatus.SUCCESS,
                        0L,
                        null,
                        "gpt",
                        "1",
                        List.of(new PythonMenuIngredientResultDto("TOMATO", BigDecimal.valueOf(0.90)))
                )
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonClientPort);
        MealImportResult importResult = new MealImportResult(1L, 1L, List.of(22L), List.of(22L), List.of());

        service.process(importResult);

        assertThat(persistencePort.updatedMenuStatus.get(22L)).isEqualTo(MenuAiStatus.SUCCESS);
        assertThat(persistencePort.updatedSpicyLevel.get(22L)).isEqualTo(MenuSpicyLevel.LEVEL_0);
    }

    @Test
    void processInfersSuccessWhenStatusIsNullAndIngredientsExist() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(31L, "Tempura");

        FakePythonMealClientPort pythonClientPort = new FakePythonMealClientPort();
        pythonClientPort.analysisResponse = new PythonMenuAnalysisResponse(List.of(
                new PythonMenuAnalysisResultDto(
                        31L,
                        "Tempura",
                        null,
                        null,
                        "gpt",
                        "1",
                        List.of(new PythonMenuIngredientResultDto("SHRIMP", BigDecimal.valueOf(0.88)))
                )
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonClientPort);
        MealImportResult importResult = new MealImportResult(1L, 1L, List.of(31L), List.of(31L), List.of());

        service.process(importResult);

        assertThat(persistencePort.updatedMenuStatus.get(31L)).isEqualTo(MenuAiStatus.SUCCESS);
    }

    @Test
    void processInfersFailedWhenStatusIsNullAndIngredientsAreEmpty() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(32L, "Soup");

        FakePythonMealClientPort pythonClientPort = new FakePythonMealClientPort();
        pythonClientPort.analysisResponse = new PythonMenuAnalysisResponse(List.of(
                new PythonMenuAnalysisResultDto(
                        32L,
                        "Soup",
                        null,
                        null,
                        "gpt",
                        "1",
                        List.of()
                )
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonClientPort);
        MealImportResult importResult = new MealImportResult(1L, 1L, List.of(32L), List.of(32L), List.of());

        service.process(importResult);

        assertThat(persistencePort.updatedMenuStatus.get(32L)).isEqualTo(MenuAiStatus.FAILED);
    }

    @Test
    void processInfersFailedWhenStatusIsNullAndFailureReasonExists() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(33L, "Rice");

        FakePythonMealClientPort pythonClientPort = new FakePythonMealClientPort();
        pythonClientPort.analysisResponse = new PythonMenuAnalysisResponse(List.of(
                new PythonMenuAnalysisResultDto(
                        33L,
                        "Rice",
                        null,
                        "analysis failed",
                        "gpt",
                        "1",
                        List.of()
                )
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonClientPort);
        MealImportResult importResult = new MealImportResult(1L, 1L, List.of(33L), List.of(33L), List.of());

        service.process(importResult);

        assertThat(persistencePort.updatedMenuStatus.get(33L)).isEqualTo(MenuAiStatus.FAILED);
    }

    @Test
    void processPassesOnlyValidDeduplicatedAllergiesToPersistence() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        persistencePort.menuNames.put(41L, "Noodles");
        persistencePort.existingAllergyCodes = Set.of("EGG", "MILK");

        FakePythonMealClientPort pythonClientPort = new FakePythonMealClientPort();
        pythonClientPort.analysisResponse = new PythonMenuAnalysisResponse(List.of(
                new PythonMenuAnalysisResultDto(
                        41L,
                        "Noodles",
                        PythonMenuAnalysisStatus.SUCCESS,
                        null,
                        "gpt",
                        "1",
                        List.of(),
                        List.of(
                                new PythonMenuAllergyResultDto("EGG", BigDecimal.valueOf(0.91)),
                                new PythonMenuAllergyResultDto("EGG", BigDecimal.valueOf(0.93)),
                                new PythonMenuAllergyResultDto("UNKNOWN", BigDecimal.valueOf(0.22)),
                                new PythonMenuAllergyResultDto("MILK", null)
                        )
                )
        ));

        MenuAiAnalysisFollowUpService service = new MenuAiAnalysisFollowUpService(persistencePort, pythonClientPort);
        MealImportResult importResult = new MealImportResult(1L, 1L, List.of(41L), List.of(41L), List.of());

        service.process(importResult);

        assertThat(persistencePort.savedAllergyCodesByMenuId.get(41L)).containsExactlyInAnyOrder("EGG", "MILK");
        assertThat(persistencePort.updatedMenuStatus.get(41L)).isEqualTo(MenuAiStatus.SUCCESS);
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
        private final Map<Long, MenuAiStatus> updatedMenuStatus = new HashMap<>();
        private final Map<Long, MenuSpicyLevel> updatedSpicyLevel = new HashMap<>();
        private final Map<Long, Set<String>> savedAllergyCodesByMenuId = new HashMap<>();
        private Set<String> existingAllergyCodes = Set.of();

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
        public List<RestrictionIngredientRow> findReligiousRestrictionIngredients(String religiousCode) {
            return List.of();
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
        public void saveMenuAnalysis(Long menuId, MenuAiStatus status, String modelName, String modelVersion, String reason, LocalDateTime analyzedAt, List<MenuIngredientCandidate> ingredients) {
            savedAnalysisMenuIds.add(menuId);
        }

        @Override
        public Set<String> findExistingAllergyCodes(Set<String> allergyCodes) {
            if (allergyCodes == null || allergyCodes.isEmpty()) {
                return Set.of();
            }
            return allergyCodes.stream().filter(existingAllergyCodes::contains).collect(java.util.stream.Collectors.toSet());
        }

        @Override
        public void updateMenuAiStatus(Long menuId, MenuAiStatus aiStatus, LocalDateTime analyzedAt) {
            updatedMenuStatus.put(menuId, aiStatus);
        }

        @Override
        public void updateMenuAiStatus(Long menuId, MenuAiStatus aiStatus, LocalDateTime analyzedAt, MenuSpicyLevel spicyLevel) {
            updatedMenuStatus.put(menuId, aiStatus);
            updatedSpicyLevel.put(menuId, spicyLevel);
        }

        @Override
        public void saveMenuAnalysisAndUpdateStatus(
                Long menuId,
                MenuAiStatus status,
                String modelName,
                String modelVersion,
                String reason,
                LocalDateTime analyzedAt,
                List<MenuIngredientCandidate> ingredients,
                Set<String> validIngredientCodes,
                List<com.mealguide.mealguide_api.mealcrawl.domain.MenuAllergyCandidate> allergies,
                Set<String> validAllergyCodes,
                MenuSpicyLevel spicyLevel
        ) {
            savedAnalysisMenuIds.add(menuId);
            updatedMenuStatus.put(menuId, status);
            updatedSpicyLevel.put(menuId, spicyLevel);
            Set<String> validCodes = validAllergyCodes == null ? Set.of() : validAllergyCodes;
            Set<String> deduplicated = allergies == null
                    ? Set.of()
                    : allergies.stream()
                    .map(allergy -> allergy.allergyCode() == null ? null : allergy.allergyCode().trim())
                    .filter(code -> code != null && !code.isBlank() && validCodes.contains(code))
                    .collect(java.util.stream.Collectors.toSet());
            savedAllergyCodesByMenuId.put(menuId, deduplicated);
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

