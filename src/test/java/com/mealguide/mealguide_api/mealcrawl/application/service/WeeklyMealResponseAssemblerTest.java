package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailBaseCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRiskDataCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.ReligionIngredientMapCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.ReligionIngredientMappingRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealI18nCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealRiskDataCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuReadCachePort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.WeeklyMealResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyMealResponseAssemblerTest {

    @Test
    void confirmedAllergyMatchReturnsDanger() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.confirmedIngredients = List.of(new MealMenuIngredientRow(11L, "PORK", "Pork"));
        port.allergyRiskMealMenuIds = Set.of(11L);

        WeeklyMealResponseAssembler assembler = assembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("DANGER");
    }

    @Test
    void confirmedReligionMatchReturnsDanger() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.confirmedIngredients = List.of(new MealMenuIngredientRow(11L, "PORK", "Pork"));
        port.religionRestrictions = List.of(new RestrictionIngredientRow("HALAL", "PORK", "Pork"));

        WeeklyMealResponseAssembler assembler = assembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("DANGER");
    }

    @Test
    void aiAllergyMatchReturnsDanger() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.aiIngredients = List.of(new MealMenuIngredientRow(11L, "PORK", "Pork"));
        port.allergyRiskMealMenuIds = Set.of(11L);

        WeeklyMealResponseAssembler assembler = assembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("DANGER");
    }

    @Test
    void aiReligionMatchReturnsDanger() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.aiIngredients = List.of(new MealMenuIngredientRow(11L, "PORK", "Pork"));
        port.religionRestrictions = List.of(new RestrictionIngredientRow("HALAL", "PORK", "Pork"));

        WeeklyMealResponseAssembler assembler = assembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("DANGER");
    }

    @Test
    void noIngredientInfoReturnsUnknown() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        WeeklyMealResponseAssembler assembler = assembler(port);

        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("UNKNOWN");
    }

    @Test
    void ingredientExistsWithoutMatchReturnsSafeAndNoMenuIdField() throws Exception {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.confirmedIngredients = List.of(new MealMenuIngredientRow(11L, "RICE", "Rice"));

        WeeklyMealResponseAssembler assembler = assembler(port);
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

        WeeklyMealResponseAssembler assembler = assembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), samplePreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).menuName()).isEqualTo("Kimchi Stew EN");
    }

    @Test
    void nullMealMenuIdDoesNotFailOnEmptyRiskOrTranslationMaps() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        WeeklyMealResponseAssembler assembler = assembler(port);

        WeeklyMealResponse response = assembler.assemble(payloadWithNullMealMenuId(), samplePreference());

        WeeklyMealResponse.MenuResponse menu = response.mealSchedules().get(0).menus().get(0);
        assertThat(menu.mealMenuId()).isNull();
        assertThat(menu.menuName()).isEqualTo("Unknown Menu");
        assertThat(menu.risk().riskLevel()).isEqualTo("UNKNOWN");
    }

    @Test
    void koreanLanguageStillReturnsRiskLevelOnly() {
        FakeMealCrawlPersistencePort port = new FakeMealCrawlPersistencePort();
        port.confirmedIngredients = List.of(new MealMenuIngredientRow(11L, "PORK", "Pork"));
        port.allergyRiskMealMenuIds = Set.of(11L);

        WeeklyMealResponseAssembler assembler = assembler(port);
        WeeklyMealResponse response = assembler.assemble(samplePayload(), koreanPreference());

        assertThat(response.mealSchedules().get(0).menus().get(0).risk().riskLevel()).isEqualTo("DANGER");
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

    private WeeklyMealCachePayload payloadWithNullMealMenuId() {
        return new WeeklyMealCachePayload(
                1L,
                10L,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                List.of(new WeeklyMealCachePayload.MealScheduleItem(
                        LocalDate.of(2026, 4, 20),
                        "LUNCH",
                        List.of(new WeeklyMealCachePayload.MenuItem(
                                null,
                                "Unknown Menu",
                                "Korean",
                                1,
                                2L,
                                false
                        ))
                ))
        );
    }

    private CurrentUserMealPreference samplePreference() {
        return new CurrentUserMealPreference(
                100L,
                1L,
                "en",
                List.of("HALAL"),
                List.of("PORK")
        );
    }

    private CurrentUserMealPreference koreanPreference() {
        return new CurrentUserMealPreference(
                100L,
                1L,
                "ko",
                List.of("HALAL"),
                List.of("PORK")
        );
    }

    private RiskLevelPolicyResolver defaultRiskResolver() {
        return new RiskLevelPolicyResolver(new MealCrawlProperties());
    }

    private WeeklyMealResponseAssembler assembler(FakeMealCrawlPersistencePort port) {
        return new WeeklyMealResponseAssembler(
                port,
                new FakeMenuReadCachePort(),
                new MealCrawlProperties(),
                defaultRiskResolver()
        );
    }

    private static class FakeMealCrawlPersistencePort implements MealCrawlPersistencePort {
        private Map<Long, String> translatedMenuNames = Map.of();
        private List<MealMenuIngredientRow> confirmedIngredients = List.of();
        private List<MealMenuIngredientRow> aiIngredients = List.of();
        private Set<Long> allergyRiskMealMenuIds = Set.of();
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
            return Set.of();
        }

        @Override
        public List<MealMenuIngredientRow> findAiIngredientsByMealMenuIds(Set<Long> mealMenuIds) {
            return aiIngredients;
        }

        @Override
        public Set<Long> findMealMenuIdsHavingAiIngredients(Set<Long> mealMenuIds) {
            return Set.of();
        }

        @Override
        public List<RestrictionIngredientRow> findReligiousRestrictionIngredients(List<String> religiousCodes) {
            return religionRestrictions;
        }

        @Override
        public List<MealMenuAllergyRow> findAllergiesByMealMenuIds(Set<Long> mealMenuIds, String langCode) {
            return allergyRiskMealMenuIds.stream()
                    .map(mealMenuId -> new MealMenuAllergyRow(mealMenuId, "PORK", "Pork", null))
                    .toList();
        }

        @Override
        public List<ReligionIngredientMappingRow> findReligionIngredientMappings() {
            return religionRestrictions.stream()
                    .map(row -> new ReligionIngredientMappingRow(
                            row.ingredientCode(),
                            row.restrictionCode(),
                            row.ingredientName(),
                            row.ingredientName()
                    ))
                    .toList();
        }

        @Override
        public Set<Long> findAnalyzedMenuIds(Set<Long> menuIds) {
            return Set.of();
        }

        @Override
        public List<Long> findRetryTargetMenuIds(int limit, int maxAttemptCount) {
            return List.of();
        }

        @Override
        public Map<Long, String> findMenuNamesByIds(Set<Long> menuIds) {
            return new HashMap<>();
        }

        @Override
        public void saveMenuAnalysis(Long menuId, MenuAiStatus status, String modelName, String modelVersion, String reason, LocalDateTime analyzedAt, int attemptCount, List<MenuIngredientCandidate> ingredients) {
        }

        @Override
        public void updateMenuAiStatus(Long menuId, MenuAiStatus aiStatus, LocalDateTime analyzedAt) {
        }

        @Override
        public Set<MenuTranslationKey> findExistingMenuTranslationKeys(Set<Long> menuIds, List<String> langCodes) {
            return Set.of();
        }

        @Override
        public void saveMenuTranslation(Long menuId, String langCode, String translatedName) {
        }
    }

    private static class FakeMenuReadCachePort implements MenuReadCachePort {

        @Override
        public Optional<WeeklyMealRiskDataCachePayload> findWeeklyMealRiskData(Long cafeteriaId, LocalDate weekStartDate) {
            return Optional.empty();
        }

        @Override
        public void upsertWeeklyMealRiskData(Long cafeteriaId, LocalDate weekStartDate, WeeklyMealRiskDataCachePayload payload, Duration ttl) {
        }

        @Override
        public Optional<WeeklyMealI18nCachePayload> findWeeklyMealI18n(Long cafeteriaId, LocalDate weekStartDate, String langCode) {
            return Optional.empty();
        }

        @Override
        public void upsertWeeklyMealI18n(Long cafeteriaId, LocalDate weekStartDate, String langCode, WeeklyMealI18nCachePayload payload, Duration ttl) {
        }

        @Override
        public Optional<MenuDetailBaseCachePayload> findMenuDetailBase(Long mealMenuId, String langCode) {
            return Optional.empty();
        }

        @Override
        public void upsertMenuDetailBase(Long mealMenuId, String langCode, MenuDetailBaseCachePayload payload, Duration ttl) {
        }

        @Override
        public Optional<MenuDetailRiskDataCachePayload> findMenuDetailRiskData(Long mealMenuId) {
            return Optional.empty();
        }

        @Override
        public void upsertMenuDetailRiskData(Long mealMenuId, MenuDetailRiskDataCachePayload payload, Duration ttl) {
        }

        @Override
        public Optional<ReligionIngredientMapCachePayload> findReligionIngredientMap() {
            return Optional.empty();
        }

        @Override
        public void upsertReligionIngredientMap(ReligionIngredientMapCachePayload payload, Duration ttl) {
        }
    }
}
