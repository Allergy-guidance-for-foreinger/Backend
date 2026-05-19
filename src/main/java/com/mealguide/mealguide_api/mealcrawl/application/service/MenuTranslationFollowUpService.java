package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.PythonMealClientException;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonTranslatedMenuNameDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuTranslationFollowUpService {

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final PythonMealClientPort pythonMealClientPort;
    private final MealCrawlProperties mealCrawlProperties;

    public void process(MealImportResult importResult) {
        process("manual", null, null, null, importResult);
    }

    public void process(String runId, Long schoolId, Long cafeteriaId, LocalDate weekStartDate, MealImportResult importResult) {
        Instant startedAt = Instant.now();
        Set<Long> targetMenuIds = new HashSet<>(importResult.menusNeedingTranslation());
        if (targetMenuIds.isEmpty()) {
            log.info(
                    "event=SKIP stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} reason=no-target-menus",
                    runId, schoolId, cafeteriaId, weekStartDate
            );
            return;
        }

        List<String> targetLanguages = normalizeTargetLanguages(mealCrawlProperties.getTranslationTargetLanguages());
        if (targetLanguages == null || targetLanguages.isEmpty()) {
            log.info(
                    "event=SKIP stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} reason=no-target-languages",
                    runId, schoolId, cafeteriaId, weekStartDate
            );
            return;
        }

        Set<MenuTranslationKey> existingKeys = mealCrawlPersistencePort.findExistingMenuTranslationKeys(targetMenuIds, targetLanguages);
        Map<Long, String> menuNames = mealCrawlPersistencePort.findMenuNamesByIds(targetMenuIds);

        List<PythonMenuTranslationTargetDto> translationTargets = menuNames.entrySet().stream()
                .filter(entry -> !isBlank(entry.getValue()))
                .filter(entry -> hasMissingTranslation(entry.getKey(), existingKeys, targetLanguages))
                .map(entry -> new PythonMenuTranslationTargetDto(entry.getKey(), entry.getValue().trim()))
                .toList();

        if (translationTargets.isEmpty()) {
            log.info(
                    "event=SKIP stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} reason=no-translation-targets targetMenuCount={} existingKeyCount={} menuNameCount={} targetLanguages={}",
                    runId,
                    schoolId,
                    cafeteriaId,
                    weekStartDate,
                    targetMenuIds.size(),
                    existingKeys.size(),
                    menuNames.size(),
                    targetLanguages
            );
            return;
        }

        log.info(
                "event=START stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} targetMenuCount={} requestTargetCount={} targetLanguages={}",
                runId,
                schoolId,
                cafeteriaId,
                weekStartDate,
                targetMenuIds.size(),
                translationTargets.size(),
                targetLanguages
        );
        int savedCount = 0;
        int skippedInvalidResult = 0;
        int skippedEmptyTranslations = 0;
        int skippedInvalidTranslation = 0;
        int skippedLangMismatch = 0;
        int skippedExistingKey = 0;
        int responseResultCount = 0;
        int batchFailureCount = 0;
        Map<MenuTranslationKey, String> translationsToSave = new LinkedHashMap<>();
        int batchSize = mealCrawlProperties.getTranslationBatchSize();
        for (int start = 0; start < translationTargets.size(); start += batchSize) {
            int end = Math.min(start + batchSize, translationTargets.size());
            List<PythonMenuTranslationTargetDto> batchTargets = translationTargets.subList(start, end);
            List<PythonMenuTranslationResultDto> results;
            try {
                PythonMenuTranslationResponse response = pythonMealClientPort.translateMenus(
                        new PythonMenuTranslationRequest(batchTargets, targetLanguages)
                );
                if (response == null) {
                    batchFailureCount++;
                    log.warn(
                            "event=FAIL stage=translation_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} batchStart={} batchSize={} message=null-translation-response",
                            runId,
                            schoolId,
                            cafeteriaId,
                            weekStartDate,
                            start,
                            batchTargets.size()
                    );
                    continue;
                }
                results = response.results() == null ? List.of() : response.results();
            } catch (PythonMealClientException exception) {
                batchFailureCount++;
                log.warn(
                        "event=FAIL stage=translation_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} batchStart={} batchSize={} status={} message={}",
                        runId,
                        schoolId,
                        cafeteriaId,
                        weekStartDate,
                        start,
                        batchTargets.size(),
                        exception.getHttpStatus(),
                        exception.getMessage(),
                        exception
                );
                continue;
            } catch (Exception exception) {
                batchFailureCount++;
                log.warn(
                        "event=FAIL stage=translation_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} batchStart={} batchSize={} message={}",
                        runId,
                        schoolId,
                        cafeteriaId,
                        weekStartDate,
                        start,
                        batchTargets.size(),
                        exception.getMessage(),
                        exception
                );
                continue;
            }
            responseResultCount += results.size();

            for (PythonMenuTranslationResultDto result : results) {
                if (result == null || result.menuId() == null || !targetMenuIds.contains(result.menuId())) {
                    skippedInvalidResult++;
                    continue;
                }

                List<PythonTranslatedMenuNameDto> translations = result.translations();
                if (translations == null || translations.isEmpty()) {
                    skippedEmptyTranslations++;
                    continue;
                }

                for (PythonTranslatedMenuNameDto translation : translations) {
                    if (translation == null || isBlank(translation.langCode()) || isBlank(translation.translatedName())) {
                        skippedInvalidTranslation++;
                        continue;
                    }

                    String langCode = translation.langCode().trim();
                    if (!targetLanguages.contains(langCode)) {
                        skippedLangMismatch++;
                        continue;
                    }

                    MenuTranslationKey key = new MenuTranslationKey(result.menuId(), langCode);
                    if (existingKeys.contains(key)) {
                        skippedExistingKey++;
                        continue;
                    }

                    translationsToSave.put(key, translation.translatedName().trim());
                    existingKeys.add(key);
                    savedCount++;
                }
            }
        }
        mealCrawlPersistencePort.saveMenuTranslations(translationsToSave);
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        int failCount = skippedInvalidResult + skippedEmptyTranslations + skippedInvalidTranslation + skippedLangMismatch + skippedExistingKey + batchFailureCount;
        int successRate = savedCount + failCount == 0 ? 100 : (savedCount * 100) / (savedCount + failCount);

        log.info(
                "event=END stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} responseResultCount={} savedCount={} skippedInvalidResultCount={} skippedEmptyTranslationsCount={} skippedInvalidTranslationCount={} skippedLangMismatchCount={} skippedExistingKeyCount={} batchFailureCount={} failCount={} successRate={} durationMs={} result={}",
                runId,
                schoolId,
                cafeteriaId,
                weekStartDate,
                responseResultCount,
                savedCount,
                skippedInvalidResult,
                skippedEmptyTranslations,
                skippedInvalidTranslation,
                skippedLangMismatch,
                skippedExistingKey,
                batchFailureCount,
                failCount,
                successRate,
                durationMs,
                failCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS"
        );
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

    private List<String> normalizeTargetLanguages(List<String> targetLanguages) {
        if (targetLanguages == null || targetLanguages.isEmpty()) {
            return List.of();
        }

        return targetLanguages.stream()
                .filter(langCode -> !isBlank(langCode))
                .map(String::trim)
                .distinct()
                .toList();
    }
}

