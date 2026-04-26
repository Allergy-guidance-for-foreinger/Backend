package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.WeeklyMealResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyMealResponseAssemblerTest {

    @Test
    void confirmedAllergyMatchReturnsDanger() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.confirmedMealMenuIds = Set.of(11L);
        port.confirmedIngredients = List.of(new MealMenuIngredientRow(11L, "PORK", "Pork"));
        port.allergyRestrictions = List.of(new RestrictionIngredientRow("PORK", "PORK", "Pork"));

        WeeklyMealResponseAssembler assembler = new WeeklyMealResponseAssembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("DANGER");
        assertThat(response.mealSchedules().get(0).menus().get(0).risk().reasons().get(0).message())
                .isEqualTo("Allergy risk detected for this menu.");
    }

    @Test
    void confirmedReligionMatchReturnsDanger() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.confirmedMealMenuIds = Set.of(11L);
        port.confirmedIngredients = List.of(new MealMenuIngredientRow(11L, "PORK", "Pork"));
        port.religionRestrictions = List.of(new RestrictionIngredientRow("HALAL", "PORK", "Pork"));

        WeeklyMealResponseAssembler assembler = new WeeklyMealResponseAssembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("DANGER");
    }

    @Test
    void aiOnlyMatchReturnsCaution() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.aiMealMenuIds = Set.of(11L);
        port.aiIngredients = List.of(new MealMenuIngredientRow(11L, "PORK", "Pork"));
        port.allergyRestrictions = List.of(new RestrictionIngredientRow("PORK", "PORK", "Pork"));

        WeeklyMealResponseAssembler assembler = new WeeklyMealResponseAssembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("CAUTION");
    }

    @Test
    void noIngredientInfoReturnsUnknown() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        WeeklyMealResponseAssembler assembler = new WeeklyMealResponseAssembler(port);

        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("UNKNOWN");
    }

    @Test
    void ingredientExistsWithoutMatchReturnsSafeAndNoMenuIdField() throws Exception {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.confirmedMealMenuIds = Set.of(11L);
        port.confirmedIngredients = List.of(new MealMenuIngredientRow(11L, "RICE", "Rice"));

        WeeklyMealResponseAssembler assembler = new WeeklyMealResponseAssembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        WeeklyMealResponse.MenuResponse menu = response.mealSchedules().get(0).menus().get(0);
        assertThat(menu.risk().riskLevel()).isEqualTo("SAFE");
        String json = new ObjectMapper().writeValueAsString(menu);
        assertThat(json).contains("\"mealMenuId\"");
        assertThat(json).doesNotContain("\"menuId\"");
    }

    @Test
    void appliesTranslatedMenuNameWhenUserLanguageIsNotKorean() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.translatedMenuNames = Map.of(11L, "Kimchi Stew EN");

        WeeklyMealResponseAssembler assembler = new WeeklyMealResponseAssembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).menuName()).isEqualTo("Kimchi Stew EN");
    }

    @Test
    void returnsKoreanRiskMessageWhenUserLanguageIsKorean() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.confirmedIngredients = List.of(new MealMenuIngredientRow(11L, "PORK", "Pork"));
        port.allergyRestrictions = List.of(new RestrictionIngredientRow("PORK", "PORK", "Pork"));

        WeeklyMealResponseAssembler assembler = new WeeklyMealResponseAssembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), koreanPreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().reasons().get(0).message())
                .isEqualTo("이 메뉴에서 알레르기 위험 성분이 확인되었습니다.");
    }

    private WeeklyMealCachePayload samplePayload() {
        return new WeeklyMealCachePayload(
                1L,
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

    private CurrentUserMealPreference samplePreference() {
        return new CurrentUserMealPreference(
                100L,
                1L,
                "en",
                "HALAL",
                List.of("PORK")
        );
    }

    private CurrentUserMealPreference koreanPreference() {
        return new CurrentUserMealPreference(
                100L,
                1L,
                "ko",
                "HALAL",
                List.of("PORK")
        );
    }

    private static class FakeMealCrawlPersistencePort implements MealCrawlPersistencePort {
        private Map<Long, String> translatedMenuNames = Map.of();
        private Set<Long> confirmedMealMenuIds = Set.of();
        private List<MealMenuIngredientRow> confirmedIngredients = List.of();
        private Set<Long> aiMealMenuIds = Set.of();
        private List<MealMenuIngredientRow> aiIngredients = List.of();
        private List<RestrictionIngredientRow> allergyRestrictions = List.of();
        private List<RestrictionIngredientRow> religionRestrictions = List.of();

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
            return translatedMenuNames;
        }

        @Override
        public List<MealMenuIngredientRow> findConfirmedIngredientsByMealMenuIds(Set<Long> mealMenuIds) {
            return confirmedIngredients;
        }

        @Override
        public Set<Long> findMealMenuIdsHavingConfirmedIngredients(Set<Long> mealMenuIds) {
            return confirmedMealMenuIds;
        }

        @Override
        public List<MealMenuIngredientRow> findAiIngredientsByMealMenuIds(Set<Long> mealMenuIds) {
            return aiIngredients;
        }

        @Override
        public Set<Long> findMealMenuIdsHavingAiIngredients(Set<Long> mealMenuIds) {
            return aiMealMenuIds;
        }

        @Override
        public List<RestrictionIngredientRow> findAllergyRestrictionIngredients(Set<String> allergyCodes) {
            return allergyRestrictions;
        }

        @Override
        public List<RestrictionIngredientRow> findReligiousRestrictionIngredients(String religiousCode) {
            return religionRestrictions;
        }

        @Override
        public Set<Long> findAnalyzedMenuIds(Set<Long> menuIds) {
            return Set.of();
        }

        @Override
        public Map<Long, String> findMenuNamesByIds(Set<Long> menuIds) {
            return new HashMap<>();
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
