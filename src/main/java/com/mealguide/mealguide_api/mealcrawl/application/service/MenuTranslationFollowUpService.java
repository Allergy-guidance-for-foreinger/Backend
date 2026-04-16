package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonTranslatedMenuNameDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MenuTranslationFollowUpService {

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final PythonMealClientPort pythonMealClientPort;
    private final MealCrawlProperties mealCrawlProperties;

    @Transactional
    public void process(MealImportResult importResult) {
        Set<Long> targetMenuIds = new HashSet<>(importResult.menusNeedingTranslation());
        if (targetMenuIds.isEmpty()) {
            return;
        }

        List<String> targetLanguages = mealCrawlProperties.getTranslationTargetLanguages();
        if (targetLanguages == null || targetLanguages.isEmpty()) {
            return;
        }

        Set<MenuTranslationKey> existingKeys = mealCrawlPersistencePort.findExistingMenuTranslationKeys(targetMenuIds, targetLanguages);
        Map<Long, String> menuNames = mealCrawlPersistencePort.findMenuNamesByIds(targetMenuIds);

        List<PythonMenuTranslationTargetDto> translationTargets = menuNames.entrySet().stream()
                .filter(entry -> hasMissingTranslation(entry.getKey(), existingKeys, targetLanguages))
                .map(entry -> new PythonMenuTranslationTargetDto(entry.getKey(), entry.getValue()))
                .toList();

        if (translationTargets.isEmpty()) {
            return;
        }

        PythonMenuTranslationResponse response = pythonMealClientPort.translateMenus(
                new PythonMenuTranslationRequest(translationTargets, targetLanguages)
        );

        List<PythonMenuTranslationResultDto> results = response.results() == null ? List.of() : response.results();
        for (PythonMenuTranslationResultDto result : results) {
            if (result == null || result.menuId() == null || !targetMenuIds.contains(result.menuId())) {
                continue;
            }

            List<PythonTranslatedMenuNameDto> translations = result.translations();
            if (translations == null || translations.isEmpty()) {
                continue;
            }

            for (PythonTranslatedMenuNameDto translation : translations) {
                if (translation == null || isBlank(translation.langCode()) || isBlank(translation.translatedName())) {
                    continue;
                }

                String langCode = translation.langCode().trim();
                if (!targetLanguages.contains(langCode)) {
                    continue;
                }

                MenuTranslationKey key = new MenuTranslationKey(result.menuId(), langCode);
                if (existingKeys.contains(key)) {
                    continue;
                }

                mealCrawlPersistencePort.saveMenuTranslation(result.menuId(), langCode, translation.translatedName().trim());
                existingKeys.add(key);
            }
        }
    }

    private boolean hasMissingTranslation(Long menuId, Set<MenuTranslationKey> existingKeys, List<String> targetLanguages) {
        for (String langCode : targetLanguages) {
            if (!existingKeys.contains(new MenuTranslationKey(menuId, langCode))) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

