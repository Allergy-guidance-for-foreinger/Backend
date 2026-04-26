package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonCrawledMenuDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonDailyMealDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
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

class MealImportServiceTest {

    @Test
    void importMealsReusesMenuAndReturnsMissingAnalysisAndTranslationTargets() {
        FakeMealCrawlPersistencePort persistencePort = new FakeMealCrawlPersistencePort();
        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setTranslationTargetLanguages(List.of("en", "ja"));
        MealImportService mealImportService = new MealImportService(persistencePort, properties);

        MealCrawlTarget target = new MealCrawlTarget(1L, 10L, "School", "Main", "http://source", LocalDate.now(), LocalDate.now().plusDays(6));

        PythonMealCrawlResponse response = new PythonMealCrawlResponse(
                "School",
                "Main",
                "http://source",
                target.startDate(),
                target.endDate(),
                List.of(
                        new PythonDailyMealDto(
                                LocalDate.now(),
                                "LUNCH",
                                List.of(
                                        new PythonCrawledMenuDto("A", 1, "Kimchi Stew"),
                                        new PythonCrawledMenuDto("B", 2, "Kimchi Stew")
                                )
                        )
                )
        );

        MealImportResult result = mealImportService.importMeals(target, response);

        assertThat(persistencePort.menuNameToId).hasSize(1);
        assertThat(persistencePort.mealScheduleKeys).hasSize(1);
        assertThat(result.importedMenuIds()).hasSize(1);
        assertThat(result.menusNeedingAnalysis()).containsExactlyElementsOf(result.importedMenuIds());
        assertThat(result.menusNeedingTranslation()).containsExactlyElementsOf(result.importedMenuIds());
    }

    private static class FakeMealCrawlPersistencePort implements MealCrawlPersistencePort {
        private final Map<String, Long> menuNameToId = new HashMap<>();
        private final Set<String> mealScheduleKeys = new HashSet<>();
        private long sequence = 1L;

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
            String key = cafeteriaId + "|" + mealDate + "|" + mealType;
            mealScheduleKeys.add(key);
            return 100L;
        }

        @Override
        public Long getOrCreateMenu(String menuName) {
            return menuNameToId.computeIfAbsent(menuName, key -> sequence++);
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
