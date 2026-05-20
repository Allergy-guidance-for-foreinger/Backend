package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationStatus;
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
        Set<Long> targetMenuIds = new HashSet<>(importResult.menusNeedingTranslation());
        List<String> targetLanguages = normalizeTargetLanguages(mealCrawlProperties.getTranslationTargetLanguages());
        processInternal(runId, schoolId, cafeteriaId, weekStartDate, targetMenuIds, targetLanguages, false, Map.of());
    }

    public void processRetryPending(String runId) {
        int limit = mealCrawlProperties.getTranslationRetryBatchSize();
        int maxAttemptCount = mealCrawlProperties.getTranslationMaxAttemptCount();
        List<MenuTranslationKey> retryTargetKeys = mealCrawlPersistencePort.findTranslationRetryTargetKeys(limit, maxAttemptCount);
        if (retryTargetKeys.isEmpty()) {
            log.info("event=SKIP stage=translation_followup runId={} retryMode=true reason=no-target-keys", runId);
            return;
        }
        Set<Long> targetMenuIds = retryTargetKeys.stream().map(MenuTranslationKey::menuId).collect(java.util.stream.Collectors.toSet());
        List<String> targetLanguages = retryTargetKeys.stream().map(MenuTranslationKey::langCode).distinct().toList();
        Map<MenuTranslationKey, Integer> latestAttemptCounts = mealCrawlPersistencePort.findLatestTranslationAttemptCounts(new HashSet<>(retryTargetKeys));
        processInternal(runId, null, null, null, targetMenuIds, targetLanguages, true, latestAttemptCounts);
    }

    private void processInternal(
            String runId,
            Long schoolId,
            Long cafeteriaId,
            LocalDate weekStartDate,
            Set<Long> targetMenuIds,
            List<String> targetLanguages,
            boolean retryMode,
            Map<MenuTranslationKey, Integer> latestAttemptCounts
    ) {
        Instant startedAt = Instant.now();
        if (targetMenuIds.isEmpty()) {
            log.info(
                    "event=SKIP stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} reason=no-target-menus",
                    runId, schoolId, cafeteriaId, weekStartDate, retryMode
            );
            return;
        }

        if (targetLanguages == null || targetLanguages.isEmpty()) {
            log.info(
                    "event=SKIP stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} reason=no-target-languages",
                    runId, schoolId, cafeteriaId, weekStartDate, retryMode
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
                    "event=SKIP stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} reason=no-translation-targets targetMenuCount={} existingKeyCount={} menuNameCount={} targetLanguages={}",
                    runId,
                    schoolId,
                    cafeteriaId,
                    weekStartDate,
                    retryMode,
                    targetMenuIds.size(),
                    existingKeys.size(),
                    menuNames.size(),
                    targetLanguages
            );
            return;
        }

        log.info(
                "event=START stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} targetMenuCount={} requestTargetCount={} targetLanguages={}",
                runId,
                schoolId,
                cafeteriaId,
                weekStartDate,
                retryMode,
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
        Map<MenuTranslationKey, String> successfulTranslations = new LinkedHashMap<>();
        int batchSize = retryMode ? mealCrawlProperties.getTranslationRetryBatchSize() : mealCrawlProperties.getTranslationBatchSize();
        for (int start = 0; start < translationTargets.size(); start += batchSize) {
            int end = Math.min(start + batchSize, translationTargets.size());
            List<PythonMenuTranslationTargetDto> batchTargets = translationTargets.subList(start, end);
            Set<Long> batchMenuIds = batchTargets.stream().map(PythonMenuTranslationTargetDto::menuId).collect(java.util.stream.Collectors.toSet());
            List<PythonMenuTranslationResultDto> results;
            try {
                PythonMenuTranslationResponse response = pythonMealClientPort.translateMenus(
                        new PythonMenuTranslationRequest(batchTargets, targetLanguages)
                );
                if (response == null) {
                    batchFailureCount++;
                    for (String langCode : targetLanguages) {
                        for (PythonMenuTranslationTargetDto target : batchTargets) {
                            saveTranslationFailure(
                                    target.menuId(),
                                    langCode,
                                    "Translation response is null",
                                    resolveAttemptCount(retryMode, latestAttemptCounts, target.menuId(), langCode)
                            );
                        }
                    }
                    log.warn(
                            "event=FAIL stage=translation_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} batchStart={} batchSize={} message=null-translation-response",
                            runId,
                            schoolId,
                            cafeteriaId,
                            weekStartDate,
                            retryMode,
                            start,
                            batchTargets.size()
                    );
                    continue;
                }
                results = response.results() == null ? List.of() : response.results();
            } catch (PythonMealClientException exception) {
                batchFailureCount++;
                String reason = buildBatchFailureReason(exception);
                for (String langCode : targetLanguages) {
                    for (PythonMenuTranslationTargetDto target : batchTargets) {
                        saveTranslationFailure(
                                target.menuId(),
                                langCode,
                                reason,
                                resolveAttemptCount(retryMode, latestAttemptCounts, target.menuId(), langCode)
                        );
                    }
                }
                log.warn(
                        "event=FAIL stage=translation_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} batchStart={} batchSize={} status={} message={}",
                        runId,
                        schoolId,
                        cafeteriaId,
                        weekStartDate,
                        retryMode,
                        start,
                        batchTargets.size(),
                        exception.getHttpStatus(),
                        exception.getMessage(),
                        exception
                );
                continue;
            } catch (Exception exception) {
                batchFailureCount++;
                for (String langCode : targetLanguages) {
                    for (PythonMenuTranslationTargetDto target : batchTargets) {
                        saveTranslationFailure(
                                target.menuId(),
                                langCode,
                                "Translation batch failed",
                                resolveAttemptCount(retryMode, latestAttemptCounts, target.menuId(), langCode)
                        );
                    }
                }
                log.warn(
                        "event=FAIL stage=translation_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} batchStart={} batchSize={} message={}",
                        runId,
                        schoolId,
                        cafeteriaId,
                        weekStartDate,
                        retryMode,
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
                    for (String langCode : targetLanguages) {
                        saveTranslationFailure(
                                result.menuId(),
                                langCode,
                                "No translated result",
                                resolveAttemptCount(retryMode, latestAttemptCounts, result.menuId(), langCode)
                        );
                    }
                    continue;
                }

                Set<String> resultLanguages = new HashSet<>();
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
                    resultLanguages.add(langCode);

                    MenuTranslationKey key = new MenuTranslationKey(result.menuId(), langCode);
                    if (existingKeys.contains(key)) {
                        skippedExistingKey++;
                        continue;
                    }

                    String translatedName = translation.translatedName().trim();
                    translationsToSave.put(key, translatedName);
                    successfulTranslations.put(key, translatedName);
                    existingKeys.add(key);
                    savedCount++;
                }

                for (String targetLang : targetLanguages) {
                    if (!resultLanguages.contains(targetLang)) {
                        saveTranslationFailure(
                                result.menuId(),
                                targetLang,
                                "Missing language translation",
                                resolveAttemptCount(retryMode, latestAttemptCounts, result.menuId(), targetLang)
                        );
                    }
                }
            }

            for (Long batchMenuId : batchMenuIds) {
                boolean included = results.stream().anyMatch(result -> result != null && batchMenuId.equals(result.menuId()));
                if (!included) {
                    for (String langCode : targetLanguages) {
                        saveTranslationFailure(
                                batchMenuId,
                                langCode,
                                "No translation response",
                                resolveAttemptCount(retryMode, latestAttemptCounts, batchMenuId, langCode)
                        );
                    }
                }
            }
        }
        mealCrawlPersistencePort.saveMenuTranslations(translationsToSave);
        for (Map.Entry<MenuTranslationKey, String> entry : successfulTranslations.entrySet()) {
            MenuTranslationKey key = entry.getKey();
            mealCrawlPersistencePort.saveMenuTranslationAnalysis(
                    key.menuId(),
                    key.langCode(),
                    MenuTranslationStatus.SUCCESS,
                    null,
                    resolveAttemptCount(retryMode, latestAttemptCounts, key.menuId(), key.langCode())
            );
        }
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        int failCount = skippedInvalidResult + skippedEmptyTranslations + skippedInvalidTranslation + skippedLangMismatch + skippedExistingKey + batchFailureCount;
        int successRate = savedCount + failCount == 0 ? 100 : (savedCount * 100) / (savedCount + failCount);

        log.info(
                "event=END stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} responseResultCount={} savedCount={} skippedInvalidResultCount={} skippedEmptyTranslationsCount={} skippedInvalidTranslationCount={} skippedLangMismatchCount={} skippedExistingKeyCount={} batchFailureCount={} failCount={} successRate={} durationMs={} result={}",
                runId,
                schoolId,
                cafeteriaId,
                weekStartDate,
                retryMode,
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

    private void saveTranslationFailure(Long menuId, String langCode, String reason, int attemptCount) {
        mealCrawlPersistencePort.saveMenuTranslationAnalysis(
                menuId,
                langCode,
                MenuTranslationStatus.FAILED,
                reason,
                attemptCount
        );
    }

    private int resolveAttemptCount(
            boolean retryMode,
            Map<MenuTranslationKey, Integer> latestAttemptCounts,
            Long menuId,
            String langCode
    ) {
        if (!retryMode) {
            return 1;
        }
        return latestAttemptCounts.getOrDefault(new MenuTranslationKey(menuId, langCode), 1) + 1;
    }

    private String buildBatchFailureReason(PythonMealClientException exception) {
        String body = exception.getResponseBody();
        if (body != null && body.length() > 500) {
            body = body.substring(0, 500);
        }
        return "Python translation request failed"
                + (exception.getHttpStatus() == null ? "" : " (status=" + exception.getHttpStatus() + ")")
                + (body == null || body.isBlank() ? "" : ": " + body);
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

