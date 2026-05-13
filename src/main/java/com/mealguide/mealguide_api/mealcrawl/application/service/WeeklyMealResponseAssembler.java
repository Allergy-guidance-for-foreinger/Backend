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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyMealResponseAssembler {

    private static final String DEFAULT_LANGUAGE_CODE = "ko";

    private final MealCrawlPersistencePort mealCrawlPersistencePort;

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

        Map<Long, List<MealMenuIngredientRow>> confirmedByMealMenuId = groupByMealMenuId(
                mealCrawlPersistencePort.findConfirmedIngredientsByMealMenuIds(mealMenuIds)
        );
        Set<Long> confirmedMealMenuIds = confirmedByMealMenuId.keySet();

        Set<Long> remainingMealMenuIds = new HashSet<>(mealMenuIds);
        remainingMealMenuIds.removeAll(confirmedMealMenuIds);
        Map<Long, List<MealMenuIngredientRow>> aiByMealMenuId = groupByMealMenuId(
                mealCrawlPersistencePort.findAiIngredientsByMealMenuIds(remainingMealMenuIds)
        );
        Set<Long> aiMealMenuIds = aiByMealMenuId.keySet();

        Set<Long> mealMenuIdsWithAllergyRisk = mealCrawlPersistencePort.findMealMenuIdsHavingMatchedAllergies(
                preference.userId(),
                mealMenuIds
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
        return loadTranslatedMenuNames(mealMenuIds, languageCode);
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
            return new WeeklyMealResponse.MenuRiskResponse(MenuRiskLevel.DANGER.name());
        }

        if (confirmedMealMenuIds.contains(mealMenuId)) {
            boolean hasReligionRisk = hasRestrictionMatch(
                    confirmedByMealMenuId.getOrDefault(mealMenuId, List.of()),
                    religionIngredientIndex
            );
            MenuRiskLevel level = hasReligionRisk ? MenuRiskLevel.DANGER : MenuRiskLevel.SAFE;
            return new WeeklyMealResponse.MenuRiskResponse(level.name());
        }

        if (aiMealMenuIds.contains(mealMenuId)) {
            boolean hasReligionRisk = hasRestrictionMatch(
                    aiByMealMenuId.getOrDefault(mealMenuId, List.of()),
                    religionIngredientIndex
            );
            MenuRiskLevel level = hasReligionRisk ? MenuRiskLevel.CAUTION : MenuRiskLevel.SAFE;
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
