package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonCrawledMenuDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonDailyMealDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MealImportService {

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final MealCrawlProperties mealCrawlProperties;

    @Transactional
    public MealImportResult importMeals(MealCrawlTarget target, PythonMealCrawlResponse response) {
        Set<Long> importedMenuIds = new HashSet<>();

        List<PythonDailyMealDto> dailyMeals = response.meals() == null ? List.of() : response.meals();
        for (PythonDailyMealDto dailyMeal : dailyMeals) {
            if (dailyMeal == null || dailyMeal.mealDate() == null || isBlank(dailyMeal.mealType())) {
                continue;
            }

            Long mealScheduleId = mealCrawlPersistencePort.getOrCreateMealSchedule(
                    target.cafeteriaId(),
                    dailyMeal.mealDate(),
                    normalize(dailyMeal.mealType())
            );

            List<PythonCrawledMenuDto> menus = dailyMeal.menus() == null ? List.of() : dailyMeal.menus();
            int fallbackOrder = 1;
            for (PythonCrawledMenuDto crawledMenu : menus) {
                if (crawledMenu == null || isBlank(crawledMenu.menuName())) {
                    continue;
                }

                Long menuId = mealCrawlPersistencePort.getOrCreateMenu(normalize(crawledMenu.menuName()));
                int displayOrder = crawledMenu.displayOrder() == null || crawledMenu.displayOrder() <= 0
                        ? fallbackOrder
                        : crawledMenu.displayOrder();

                mealCrawlPersistencePort.upsertMealMenu(
                        mealScheduleId,
                        menuId,
                        normalize(crawledMenu.cornerName()),
                        displayOrder
                );
                importedMenuIds.add(menuId);
                fallbackOrder++;
            }
        }

        Set<Long> analyzedMenuIds = mealCrawlPersistencePort.findAnalyzedMenuIds(importedMenuIds);
        List<Long> menusNeedingAnalysis = importedMenuIds.stream()
                .filter(menuId -> !analyzedMenuIds.contains(menuId))
                .toList();

        Set<String> existingTranslationKeys = mealCrawlPersistencePort.findExistingMenuTranslationKeys(
                importedMenuIds,
                mealCrawlProperties.getTranslationTargetLanguages()
        ).stream().map(key -> key.menuId() + "|" + key.langCode()).collect(java.util.stream.Collectors.toSet());

        List<Long> menusNeedingTranslation = new ArrayList<>();
        for (Long menuId : importedMenuIds) {
            boolean hasMissingTranslation = false;
            for (String langCode : mealCrawlProperties.getTranslationTargetLanguages()) {
                if (!existingTranslationKeys.contains(menuId + "|" + langCode)) {
                    hasMissingTranslation = true;
                    break;
                }
            }
            if (hasMissingTranslation) {
                menusNeedingTranslation.add(menuId);
            }
        }

        return new MealImportResult(
                target.schoolId(),
                target.cafeteriaId(),
                List.copyOf(importedMenuIds),
                menusNeedingAnalysis,
                menusNeedingTranslation
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}

