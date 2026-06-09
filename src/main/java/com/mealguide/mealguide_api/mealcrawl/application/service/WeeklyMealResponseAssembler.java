package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.ReligionIngredientMapCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.ReligionIngredientMappingRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealI18nCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealRiskDataCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuReadCachePort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuRiskLevel;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.WeeklyMealResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyMealResponseAssembler {

    private static final String DEFAULT_LANGUAGE_CODE = "ko";
    private static final BigDecimal MATCHED_CONFIDENCE = BigDecimal.ONE;
    private static final String SOURCE_CONFIRMED = "CONFIRMED";
    private static final String SOURCE_AI = "AI";

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final MenuReadCachePort menuReadCachePort;
    private final MealCrawlProperties mealCrawlProperties;
    private final RiskLevelPolicyResolver riskLevelPolicyResolver;

    public WeeklyMealResponse assemble(WeeklyMealCachePayload payload, CurrentUserMealPreference preference) {
        Map<Long, String> translatedMenuNames = resolveTranslatedMenuNames(payload, preference.languageCode());
        return assemble(payload, preference, translatedMenuNames);
    }

    public WeeklyMealResponse assemble(
            WeeklyMealCachePayload payload,
            CurrentUserMealPreference preference,
            Map<Long, String> translatedMenuNames
    ) {
        Set<Long> mealMenuIds = extractMealMenuIds(payload);
        if (mealMenuIds.isEmpty()) {
            return toWeeklyMealResponse(payload, translatedMenuNames, Map.of());
        }

        WeeklyMealRiskDataCachePayload riskData = loadWeeklyRiskData(payload, mealMenuIds);
        Map<Long, List<MealMenuIngredientRow>> confirmedByMealMenuId = toIngredientRows(riskData, SOURCE_CONFIRMED);
        Set<Long> confirmedMealMenuIds = confirmedByMealMenuId.keySet();
        Map<Long, List<MealMenuIngredientRow>> aiByMealMenuId = toIngredientRows(riskData, SOURCE_AI);
        Set<Long> aiMealMenuIds = aiByMealMenuId.keySet();

        Set<Long> mealMenuIdsWithAllergyRisk = findMealMenuIdsWithAllergyRisk(riskData, preference.allergyCodes());
        Map<String, List<RestrictionIngredientRow>> religionIngredientIndex = indexRestrictionIngredientsByIngredientCode(
                findReligiousRestrictionIngredients(preference.religiousCodes())
        );

        Map<Long, WeeklyMealResponse.MenuRiskResponse> riskByMealMenuId = new HashMap<>();
        for (Long mealMenuId : mealMenuIds) {
            WeeklyMealResponse.MenuRiskResponse risk;
            try {
                risk = evaluateMenuRisk(
                        mealMenuId,
                        confirmedMealMenuIds,
                        confirmedByMealMenuId,
                        aiMealMenuIds,
                        aiByMealMenuId,
                        mealMenuIdsWithAllergyRisk,
                        religionIngredientIndex
                );
            } catch (Exception exception) {
                log.warn("Risk evaluation failed for mealMenuId={}", mealMenuId, exception);
                risk = new WeeklyMealResponse.MenuRiskResponse(MenuRiskLevel.UNKNOWN.name());
            }
            riskByMealMenuId.put(mealMenuId, risk);
        }

        return toWeeklyMealResponse(payload, translatedMenuNames, riskByMealMenuId);
    }

    public Map<Long, String> resolveTranslatedMenuNames(WeeklyMealCachePayload payload, String languageCode) {
        Set<Long> mealMenuIds = extractMealMenuIds(payload);
        return loadTranslatedMenuNames(payload, mealMenuIds, languageCode);
    }

    private Set<Long> extractMealMenuIds(WeeklyMealCachePayload payload) {
        Set<Long> ids = new HashSet<>();
        for (WeeklyMealCachePayload.MealScheduleItem schedule : payload.mealSchedules()) {
            for (WeeklyMealCachePayload.MenuItem menu : schedule.menus()) {
                if (menu.mealMenuId() != null) {
                    ids.add(menu.mealMenuId());
                }
            }
        }
        return ids;
    }

    private WeeklyMealResponse.MenuRiskResponse evaluateMenuRisk(
            Long mealMenuId,
            Set<Long> confirmedMealMenuIds,
            Map<Long, List<MealMenuIngredientRow>> confirmedByMealMenuId,
            Set<Long> aiMealMenuIds,
            Map<Long, List<MealMenuIngredientRow>> aiByMealMenuId,
            Set<Long> mealMenuIdsWithAllergyRisk,
            Map<String, List<RestrictionIngredientRow>> religionIngredientIndex
    ) {
        if (mealMenuIdsWithAllergyRisk.contains(mealMenuId)) {
            return new WeeklyMealResponse.MenuRiskResponse(
                    riskLevelPolicyResolver.resolveAllergy(true, MATCHED_CONFIDENCE).name()
            );
        }

        if (confirmedMealMenuIds.contains(mealMenuId)) {
            boolean hasReligionRisk = hasRestrictionMatch(
                    confirmedByMealMenuId.getOrDefault(mealMenuId, List.of()),
                    religionIngredientIndex
            );
            MenuRiskLevel level = riskLevelPolicyResolver.resolveReligious(hasReligionRisk, MATCHED_CONFIDENCE);
            return new WeeklyMealResponse.MenuRiskResponse(level.name());
        }

        if (aiMealMenuIds.contains(mealMenuId)) {
            boolean hasReligionRisk = hasRestrictionMatch(
                    aiByMealMenuId.getOrDefault(mealMenuId, List.of()),
                    religionIngredientIndex
            );
            MenuRiskLevel level = riskLevelPolicyResolver.resolveReligious(hasReligionRisk, MATCHED_CONFIDENCE);
            return new WeeklyMealResponse.MenuRiskResponse(level.name());
        }

        log.debug("No ingredient information for risk evaluation: mealMenuId={}", mealMenuId);
        return new WeeklyMealResponse.MenuRiskResponse(MenuRiskLevel.UNKNOWN.name());
    }

    private Map<Long, List<MealMenuIngredientRow>> groupByMealMenuId(List<MealMenuIngredientRow> rows) {
        Map<Long, List<MealMenuIngredientRow>> grouped = new HashMap<>();
        for (MealMenuIngredientRow row : rows) {
            grouped.computeIfAbsent(row.mealMenuId(), unused -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private Map<String, List<RestrictionIngredientRow>> indexRestrictionIngredientsByIngredientCode(List<RestrictionIngredientRow> rows) {
        Map<String, List<RestrictionIngredientRow>> index = new HashMap<>();
        for (RestrictionIngredientRow row : rows) {
            index.computeIfAbsent(row.ingredientCode(), unused -> new ArrayList<>()).add(row);
        }
        return index;
    }

    private boolean hasRestrictionMatch(
            List<MealMenuIngredientRow> ingredientRows,
            Map<String, List<RestrictionIngredientRow>> restrictionIngredientIndex
    ) {
        for (MealMenuIngredientRow ingredientRow : ingredientRows) {
            String ingredientCode = ingredientRow.ingredientCode();
            if (!restrictionIngredientIndex.getOrDefault(ingredientCode, List.of()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private WeeklyMealResponse toWeeklyMealResponse(
            WeeklyMealCachePayload payload,
            Map<Long, String> translatedMenuNamesByMealMenuId,
            Map<Long, WeeklyMealResponse.MenuRiskResponse> riskByMealMenuId
    ) {
        List<WeeklyMealResponse.MealScheduleResponse> schedules = payload.mealSchedules().stream()
                .map(schedule -> new WeeklyMealResponse.MealScheduleResponse(
                        schedule.mealDate(),
                        schedule.mealType(),
                        schedule.menus().stream()
                                .map(menu -> new WeeklyMealResponse.MenuResponse(
                                        menu.mealMenuId(),
                                        translatedMenuNamesByMealMenuId.getOrDefault(menu.mealMenuId(), menu.menuName()),
                                        menu.cornerName(),
                                        menu.displayOrder(),
                                        menu.spicyLevel(),
                                        menu.aiAnalyzed(),
                                        riskByMealMenuId.getOrDefault(
                                                menu.mealMenuId(),
                                                new WeeklyMealResponse.MenuRiskResponse(MenuRiskLevel.UNKNOWN.name())
                                        )
                                ))
                                .toList()
                ))
                .toList();

        return new WeeklyMealResponse(
                payload.schoolId(),
                payload.cafeteriaId(),
                payload.weekStartDate(),
                payload.weekEndDate(),
                schedules
        );
    }

    private Map<Long, String> loadTranslatedMenuNames(WeeklyMealCachePayload payload, Set<Long> mealMenuIds, String languageCode) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return Map.of();
        }
        if (languageCode == null || languageCode.isBlank()) {
            return Map.of();
        }
        String normalizedLanguageCode = languageCode.trim().toLowerCase(Locale.ROOT);
        if (DEFAULT_LANGUAGE_CODE.equals(normalizedLanguageCode)) {
            return Map.of();
        }
        return menuReadCachePort.findWeeklyMealI18n(payload.cafeteriaId(), payload.weekStartDate(), normalizedLanguageCode)
                .map(WeeklyMealI18nCachePayload::menuNamesByMealMenuId)
                .orElseGet(() -> {
                    Map<Long, String> names = mealCrawlPersistencePort.findTranslatedMenuNamesByMealMenuIds(mealMenuIds, normalizedLanguageCode);
                    menuReadCachePort.upsertWeeklyMealI18n(
                            payload.cafeteriaId(),
                            payload.weekStartDate(),
                            normalizedLanguageCode,
                            new WeeklyMealI18nCachePayload(names),
                            readCacheTtl()
                    );
                    return names;
                });
    }

    private WeeklyMealRiskDataCachePayload loadWeeklyRiskData(WeeklyMealCachePayload payload, Set<Long> mealMenuIds) {
        return menuReadCachePort.findWeeklyMealRiskData(payload.cafeteriaId(), payload.weekStartDate())
                .orElseGet(() -> {
                    WeeklyMealRiskDataCachePayload loaded = loadWeeklyRiskDataFromDb(mealMenuIds);
                    menuReadCachePort.upsertWeeklyMealRiskData(payload.cafeteriaId(), payload.weekStartDate(), loaded, readCacheTtl());
                    return loaded;
                });
    }

    private WeeklyMealRiskDataCachePayload loadWeeklyRiskDataFromDb(Set<Long> mealMenuIds) {
        Map<Long, List<MealMenuIngredientRow>> confirmedByMealMenuId = groupByMealMenuId(
                mealCrawlPersistencePort.findConfirmedIngredientsByMealMenuIds(mealMenuIds)
        );
        Set<Long> remainingMealMenuIds = new HashSet<>(mealMenuIds);
        remainingMealMenuIds.removeAll(confirmedByMealMenuId.keySet());
        Map<Long, List<MealMenuIngredientRow>> aiByMealMenuId = groupByMealMenuId(
                mealCrawlPersistencePort.findAiIngredientsByMealMenuIds(remainingMealMenuIds)
        );
        List<MealMenuAllergyRow> allergyRows = mealCrawlPersistencePort.findAllergiesByMealMenuIds(mealMenuIds, DEFAULT_LANGUAGE_CODE);

        Map<Long, WeeklyMealRiskDataCachePayload.IngredientData> ingredients = new HashMap<>();
        confirmedByMealMenuId.forEach((mealMenuId, rows) -> ingredients.put(
                mealMenuId,
                new WeeklyMealRiskDataCachePayload.IngredientData(
                        SOURCE_CONFIRMED,
                        rows.stream().map(MealMenuIngredientRow::ingredientCode).toList()
                )
        ));
        aiByMealMenuId.forEach((mealMenuId, rows) -> ingredients.put(
                mealMenuId,
                new WeeklyMealRiskDataCachePayload.IngredientData(
                        SOURCE_AI,
                        rows.stream().map(MealMenuIngredientRow::ingredientCode).toList()
                )
        ));

        Map<Long, List<WeeklyMealRiskDataCachePayload.AllergyData>> allergies = new HashMap<>();
        for (MealMenuAllergyRow row : allergyRows) {
            allergies.computeIfAbsent(row.mealMenuId(), unused -> new ArrayList<>())
                    .add(new WeeklyMealRiskDataCachePayload.AllergyData(row.allergyCode(), row.confidence()));
        }
        return new WeeklyMealRiskDataCachePayload(ingredients, allergies);
    }

    private Map<Long, List<MealMenuIngredientRow>> toIngredientRows(WeeklyMealRiskDataCachePayload riskData, String source) {
        Map<Long, List<MealMenuIngredientRow>> result = new HashMap<>();
        if (riskData.ingredientsByMealMenuId() == null) {
            return result;
        }
        riskData.ingredientsByMealMenuId().forEach((mealMenuId, data) -> {
            if (data != null && source.equals(data.source())) {
                List<MealMenuIngredientRow> rows = data.ingredientCodes() == null ? List.of() : data.ingredientCodes().stream()
                        .map(code -> new MealMenuIngredientRow(mealMenuId, code, code))
                        .toList();
                result.put(mealMenuId, rows);
            }
        });
        return result;
    }

    private Set<Long> findMealMenuIdsWithAllergyRisk(WeeklyMealRiskDataCachePayload riskData, List<String> userAllergyCodes) {
        if (userAllergyCodes == null || userAllergyCodes.isEmpty() || riskData.allergiesByMealMenuId() == null) {
            return Set.of();
        }
        Set<String> userAllergies = new HashSet<>(userAllergyCodes);
        Set<Long> result = new HashSet<>();
        riskData.allergiesByMealMenuId().forEach((mealMenuId, allergies) -> {
            if (allergies != null && allergies.stream().anyMatch(allergy -> userAllergies.contains(allergy.allergyCode()))) {
                result.add(mealMenuId);
            }
        });
        return result;
    }

    private List<RestrictionIngredientRow> findReligiousRestrictionIngredients(List<String> religiousCodes) {
        if (religiousCodes == null || religiousCodes.isEmpty()) {
            return List.of();
        }
        Set<String> selectedCodes = new HashSet<>(religiousCodes);
        ReligionIngredientMapCachePayload religionMap = menuReadCachePort.findReligionIngredientMap()
                .orElseGet(() -> {
                    ReligionIngredientMapCachePayload loaded = loadReligionIngredientMapFromDb();
                    menuReadCachePort.upsertReligionIngredientMap(loaded, readCacheTtl());
                    return loaded;
                });
        List<RestrictionIngredientRow> rows = new ArrayList<>();
        if (religionMap.restrictionsByIngredientCode() == null) {
            return rows;
        }
        religionMap.restrictionsByIngredientCode().forEach((ingredientCode, restrictions) -> {
            if (restrictions == null) {
                return;
            }
            for (ReligionIngredientMapCachePayload.RestrictionData restriction : restrictions) {
                if (selectedCodes.contains(restriction.restrictionCode())) {
                    rows.add(new RestrictionIngredientRow(
                            restriction.restrictionCode(),
                            ingredientCode,
                            ingredientCode
                    ));
                }
            }
        });
        return rows;
    }

    private ReligionIngredientMapCachePayload loadReligionIngredientMapFromDb() {
        Map<String, List<ReligionIngredientMapCachePayload.RestrictionData>> map = new HashMap<>();
        for (ReligionIngredientMappingRow row : mealCrawlPersistencePort.findReligionIngredientMappings()) {
            map.computeIfAbsent(row.ingredientCode(), unused -> new ArrayList<>())
                    .add(new ReligionIngredientMapCachePayload.RestrictionData(
                            row.restrictionCode(),
                            Map.of("ko", row.koreanName(), "en", row.englishName())
                    ));
        }
        return new ReligionIngredientMapCachePayload(map);
    }

    private Duration readCacheTtl() {
        return Duration.ofSeconds(mealCrawlProperties.getReadCacheTtlSeconds());
    }
}
