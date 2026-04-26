package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCachePayload;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuRiskLevel;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.WeeklyMealResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyMealResponseAssembler {

    private static final String DEFAULT_LANGUAGE_CODE = "ko";
    private static final String REASON_TYPE_ALLERGY = "ALLERGY";
    private static final String REASON_TYPE_RELIGION = "RELIGION";
    private static final String REASON_SOURCE_CONFIRMED = "CONFIRMED";
    private static final String REASON_SOURCE_AI = "AI";

    private final MealCrawlPersistencePort mealCrawlPersistencePort;

    public WeeklyMealResponse assemble(WeeklyMealCachePayload payload, CurrentUserMealPreference preference) {
        Set<Long> mealMenuIds = extractMealMenuIds(payload);
        Map<Long, String> translatedMenuNames = loadTranslatedMenuNames(mealMenuIds, preference.languageCode());
        if (mealMenuIds.isEmpty()) {
            return toWeeklyMealResponse(payload, translatedMenuNames, Map.of());
        }

        Set<Long> confirmedMealMenuIds = mealCrawlPersistencePort.findMealMenuIdsHavingConfirmedIngredients(mealMenuIds);
        Map<Long, List<MealMenuIngredientRow>> confirmedByMealMenuId = groupByMealMenuId(
                mealCrawlPersistencePort.findConfirmedIngredientsByMealMenuIds(confirmedMealMenuIds)
        );

        Set<Long> aiMealMenuIds = new HashSet<>(mealCrawlPersistencePort.findMealMenuIdsHavingAiIngredients(mealMenuIds));
        aiMealMenuIds.removeAll(confirmedMealMenuIds);
        Map<Long, List<MealMenuIngredientRow>> aiByMealMenuId = groupByMealMenuId(
                mealCrawlPersistencePort.findAiIngredientsByMealMenuIds(aiMealMenuIds)
        );

        Map<String, List<RestrictionIngredientRow>> allergyIngredientIndex = indexRestrictionIngredientsByIngredientCode(
                mealCrawlPersistencePort.findAllergyRestrictionIngredients(Set.copyOf(preference.allergyCodes()))
        );
        Map<String, List<RestrictionIngredientRow>> religionIngredientIndex = indexRestrictionIngredientsByIngredientCode(
                mealCrawlPersistencePort.findReligiousRestrictionIngredients(preference.religiousCode())
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
                        allergyIngredientIndex,
                        religionIngredientIndex
                );
            } catch (Exception exception) {
                log.warn("Risk evaluation failed for mealMenuId={}", mealMenuId, exception);
                risk = new WeeklyMealResponse.MenuRiskResponse(MenuRiskLevel.UNKNOWN.name(), List.of());
            }
            riskByMealMenuId.put(mealMenuId, risk);
        }

        return toWeeklyMealResponse(payload, translatedMenuNames, riskByMealMenuId);
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
            Map<String, List<RestrictionIngredientRow>> allergyIngredientIndex,
            Map<String, List<RestrictionIngredientRow>> religionIngredientIndex
    ) {
        if (confirmedMealMenuIds.contains(mealMenuId)) {
            List<WeeklyMealResponse.RiskReasonResponse> reasons = buildReasons(
                    confirmedByMealMenuId.getOrDefault(mealMenuId, List.of()),
                    allergyIngredientIndex,
                    religionIngredientIndex,
                    REASON_SOURCE_CONFIRMED
            );
            MenuRiskLevel level = reasons.isEmpty() ? MenuRiskLevel.SAFE : MenuRiskLevel.DANGER;
            return new WeeklyMealResponse.MenuRiskResponse(level.name(), reasons);
        }

        if (aiMealMenuIds.contains(mealMenuId)) {
            List<WeeklyMealResponse.RiskReasonResponse> reasons = buildReasons(
                    aiByMealMenuId.getOrDefault(mealMenuId, List.of()),
                    allergyIngredientIndex,
                    religionIngredientIndex,
                    REASON_SOURCE_AI
            );
            MenuRiskLevel level = reasons.isEmpty() ? MenuRiskLevel.SAFE : MenuRiskLevel.CAUTION;
            return new WeeklyMealResponse.MenuRiskResponse(level.name(), reasons);
        }

        log.info("No ingredient information for risk evaluation: mealMenuId={}", mealMenuId);
        return new WeeklyMealResponse.MenuRiskResponse(MenuRiskLevel.UNKNOWN.name(), List.of());
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

    private List<WeeklyMealResponse.RiskReasonResponse> buildReasons(
            List<MealMenuIngredientRow> ingredientRows,
            Map<String, List<RestrictionIngredientRow>> allergyIngredientIndex,
            Map<String, List<RestrictionIngredientRow>> religionIngredientIndex,
            String source
    ) {
        List<WeeklyMealResponse.RiskReasonResponse> reasons = new ArrayList<>();
        Set<String> deduplicate = new HashSet<>();

        for (MealMenuIngredientRow ingredientRow : ingredientRows) {
            String ingredientCode = ingredientRow.ingredientCode();
            String ingredientName = ingredientRow.ingredientName();

            for (RestrictionIngredientRow allergyMatch : allergyIngredientIndex.getOrDefault(ingredientCode, List.of())) {
                String key = REASON_TYPE_ALLERGY + "|" + allergyMatch.restrictionCode() + "|" + ingredientCode + "|" + source;
                if (deduplicate.add(key)) {
                    reasons.add(new WeeklyMealResponse.RiskReasonResponse(
                            REASON_TYPE_ALLERGY,
                            allergyMatch.restrictionCode(),
                            ingredientName,
                            source,
                            "Allergy risk detected for this menu."
                    ));
                }
            }

            for (RestrictionIngredientRow religionMatch : religionIngredientIndex.getOrDefault(ingredientCode, List.of())) {
                String key = REASON_TYPE_RELIGION + "|" + religionMatch.restrictionCode() + "|" + ingredientCode + "|" + source;
                if (deduplicate.add(key)) {
                    reasons.add(new WeeklyMealResponse.RiskReasonResponse(
                            REASON_TYPE_RELIGION,
                            religionMatch.restrictionCode(),
                            ingredientName,
                            source,
                            "Religious restriction risk detected for this menu."
                    ));
                }
            }
        }

        return reasons;
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
                                                new WeeklyMealResponse.MenuRiskResponse(MenuRiskLevel.UNKNOWN.name(), List.of())
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

    private Map<Long, String> loadTranslatedMenuNames(Set<Long> mealMenuIds, String languageCode) {
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
        return mealCrawlPersistencePort.findTranslatedMenuNamesByMealMenuIds(mealMenuIds, normalizedLanguageCode);
    }
}
