package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuDescriptionKey;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuDescriptionStatus;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.PythonMealClientException;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuDescriptionRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuDescriptionTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuDescriptionResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuDescriptionResultDto;
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
public class MenuDescriptionFollowUpService {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final PythonMealClientPort pythonMealClientPort;
    private final MealCrawlProperties mealCrawlProperties;

    public void process(MealImportResult importResult) {
        process("manual", null, null, null, importResult);
    }

    public void process(String runId, Long schoolId, Long cafeteriaId, LocalDate weekStartDate, MealImportResult importResult) {
        Set<Long> targetMenuIds = new HashSet<>(importResult.importedMenuIds());
        List<String> targetLanguages = normalizeTargetLanguages(mealCrawlProperties.getDescriptionTargetLanguages());
        processInternal(runId, schoolId, cafeteriaId, weekStartDate, targetMenuIds, targetLanguages, false, Map.of());
    }

    public void processRetryPending(String runId) {
        int limit = mealCrawlProperties.getDescriptionRetryBatchSize();
        int maxAttemptCount = mealCrawlProperties.getDescriptionMaxAttemptCount();
        List<MenuDescriptionKey> retryTargetKeys = mealCrawlPersistencePort.findDescriptionRetryTargetKeys(limit, maxAttemptCount);
        if (retryTargetKeys.isEmpty()) {
            log.info("event=SKIP stage=description_followup runId={} retryMode=true reason=no-target-keys", runId);
            return;
        }
        Map<MenuDescriptionKey, Integer> latestAttemptCounts = mealCrawlPersistencePort.findLatestDescriptionAttemptCounts(new HashSet<>(retryTargetKeys));
        Map<String, Set<Long>> targetMenuIdsByLanguage = new LinkedHashMap<>();
        for (MenuDescriptionKey key : retryTargetKeys) {
            targetMenuIdsByLanguage
                    .computeIfAbsent(key.langCode(), ignored -> new HashSet<>())
                    .add(key.menuId());
        }
        for (Map.Entry<String, Set<Long>> entry : targetMenuIdsByLanguage.entrySet()) {
            processInternal(runId, null, null, null, entry.getValue(), List.of(entry.getKey()), true, latestAttemptCounts);
        }
    }

    private void processInternal(
            String runId,
            Long schoolId,
            Long cafeteriaId,
            LocalDate weekStartDate,
            Set<Long> targetMenuIds,
            List<String> targetLanguages,
            boolean retryMode,
            Map<MenuDescriptionKey, Integer> latestAttemptCounts
    ) {
        Instant startedAt = Instant.now();
        if (targetMenuIds == null || targetMenuIds.isEmpty()) {
            log.info(
                    "event=SKIP stage=description_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} reason=no-target-menus",
                    runId, schoolId, cafeteriaId, weekStartDate, retryMode
            );
            return;
        }
        if (targetLanguages == null || targetLanguages.isEmpty()) {
            log.info(
                    "event=SKIP stage=description_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} reason=no-target-languages",
                    runId, schoolId, cafeteriaId, weekStartDate, retryMode
            );
            return;
        }

        Set<MenuDescriptionKey> foundExistingKeys = mealCrawlPersistencePort.findExistingMenuDescriptionKeys(targetMenuIds, targetLanguages);
        Set<MenuDescriptionKey> existingKeys = foundExistingKeys == null ? new HashSet<>() : new HashSet<>(foundExistingKeys);
        Map<Long, String> menuNames = mealCrawlPersistencePort.findMenuNamesByIds(targetMenuIds);
        int requestTargetCount = 0;
        for (String langCode : targetLanguages) {
            for (Long menuId : targetMenuIds) {
                if (!existingKeys.contains(new MenuDescriptionKey(menuId, langCode)) && !isBlank(menuNames.get(menuId))) {
                    requestTargetCount++;
                }
            }
        }
        if (requestTargetCount == 0) {
            log.info(
                    "event=SKIP stage=description_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} reason=no-description-targets targetMenuCount={} existingKeyCount={} menuNameCount={} targetLanguages={}",
                    runId, schoolId, cafeteriaId, weekStartDate, retryMode, targetMenuIds.size(), existingKeys.size(), menuNames.size(), targetLanguages
            );
            return;
        }

        log.info(
                "event=START stage=description_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} targetMenuCount={} requestTargetCount={} targetLanguages={}",
                runId, schoolId, cafeteriaId, weekStartDate, retryMode, targetMenuIds.size(), requestTargetCount, targetLanguages
        );

        int responseResultCount = 0;
        int savedCount = 0;
        int skippedInvalidResult = 0;
        int skippedBlankDescription = 0;
        int skippedTooLongDescription = 0;
        int skippedMissingResponse = 0;
        int skippedExistingKey = 0;
        int batchFailureCount = 0;
        int batchSize = retryMode ? mealCrawlProperties.getDescriptionRetryBatchSize() : mealCrawlProperties.getDescriptionBatchSize();
        Map<MenuDescriptionKey, String> descriptionsToSave = new LinkedHashMap<>();
        Set<MenuDescriptionKey> successfulKeys = new HashSet<>();

        for (String langCode : targetLanguages) {
            List<PythonMenuDescriptionTargetDto> descriptionTargets = menuNames.entrySet().stream()
                    .filter(entry -> !isBlank(entry.getValue()))
                    .filter(entry -> !existingKeys.contains(new MenuDescriptionKey(entry.getKey(), langCode)))
                    .map(entry -> new PythonMenuDescriptionTargetDto(entry.getKey(), entry.getValue().trim()))
                    .toList();

            for (int start = 0; start < descriptionTargets.size(); start += batchSize) {
                int end = Math.min(start + batchSize, descriptionTargets.size());
                List<PythonMenuDescriptionTargetDto> batchTargets = descriptionTargets.subList(start, end);
                List<PythonMenuDescriptionResultDto> results;
                try {
                    PythonMenuDescriptionResponse response = pythonMealClientPort.describeMenus(
                            new PythonMenuDescriptionRequest(langCode, batchTargets)
                    );
                    if (response == null) {
                        batchFailureCount++;
                        saveBatchFailures(batchTargets, langCode, "Description response is null", retryMode, latestAttemptCounts);
                        log.warn(
                                "event=FAIL stage=description_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} langCode={} batchStart={} batchSize={} message=null-description-response",
                                runId, schoolId, cafeteriaId, weekStartDate, retryMode, langCode, start, batchTargets.size()
                        );
                        continue;
                    }
                    results = response.results() == null ? List.of() : response.results();
                } catch (PythonMealClientException exception) {
                    batchFailureCount++;
                    saveBatchFailures(batchTargets, langCode, buildBatchFailureReason(exception), retryMode, latestAttemptCounts);
                    log.warn(
                            "event=FAIL stage=description_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} langCode={} batchStart={} batchSize={} status={} message={}",
                            runId, schoolId, cafeteriaId, weekStartDate, retryMode, langCode, start, batchTargets.size(),
                            exception.getHttpStatus(), exception.getMessage(), exception
                    );
                    continue;
                } catch (Exception exception) {
                    batchFailureCount++;
                    saveBatchFailures(batchTargets, langCode, "Description batch failed", retryMode, latestAttemptCounts);
                    log.warn(
                            "event=FAIL stage=description_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} langCode={} batchStart={} batchSize={} message={}",
                            runId, schoolId, cafeteriaId, weekStartDate, retryMode, langCode, start, batchTargets.size(),
                            exception.getMessage(), exception
                    );
                    continue;
                }

                responseResultCount += results.size();
                Set<Long> respondedMenuIds = new HashSet<>();
                for (PythonMenuDescriptionResultDto result : results) {
                    if (result == null || result.menuId() == null || !targetMenuIds.contains(result.menuId())) {
                        skippedInvalidResult++;
                        continue;
                    }
                    respondedMenuIds.add(result.menuId());
                    MenuDescriptionKey key = new MenuDescriptionKey(result.menuId(), langCode);
                    if (existingKeys.contains(key)) {
                        skippedExistingKey++;
                        continue;
                    }
                    String description = result.description() == null ? null : result.description().trim();
                    if (isBlank(description)) {
                        skippedBlankDescription++;
                        saveDescriptionFailure(key, "Blank description", retryMode, latestAttemptCounts);
                        continue;
                    }
                    if (description.length() > MAX_DESCRIPTION_LENGTH) {
                        skippedTooLongDescription++;
                        saveDescriptionFailure(key, "Description exceeds 300 characters", retryMode, latestAttemptCounts);
                        continue;
                    }
                    descriptionsToSave.put(key, description);
                    successfulKeys.add(key);
                    existingKeys.add(key);
                    savedCount++;
                }

                for (PythonMenuDescriptionTargetDto target : batchTargets) {
                    if (!respondedMenuIds.contains(target.menuId())) {
                        skippedMissingResponse++;
                        saveDescriptionFailure(new MenuDescriptionKey(target.menuId(), langCode), "No description response", retryMode, latestAttemptCounts);
                    }
                }
            }
        }

        mealCrawlPersistencePort.saveMenuDescriptions(descriptionsToSave);
        for (MenuDescriptionKey key : successfulKeys) {
            mealCrawlPersistencePort.saveMenuDescriptionAnalysis(
                    key.menuId(),
                    key.langCode(),
                    MenuDescriptionStatus.SUCCESS,
                    null,
                    resolveAttemptCount(retryMode, latestAttemptCounts, key)
            );
        }
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        int failCount = skippedInvalidResult + skippedBlankDescription + skippedTooLongDescription + skippedMissingResponse + skippedExistingKey + batchFailureCount;
        int successRate = savedCount + failCount == 0 ? 100 : (savedCount * 100) / (savedCount + failCount);
        log.info(
                "event=END stage=description_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} responseResultCount={} savedCount={} skippedInvalidResultCount={} skippedBlankDescriptionCount={} skippedTooLongDescriptionCount={} skippedMissingResponseCount={} skippedExistingKeyCount={} batchFailureCount={} failCount={} successRate={} durationMs={} result={}",
                runId, schoolId, cafeteriaId, weekStartDate, retryMode, responseResultCount, savedCount,
                skippedInvalidResult, skippedBlankDescription, skippedTooLongDescription, skippedMissingResponse, skippedExistingKey,
                batchFailureCount, failCount, successRate, durationMs, failCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS"
        );
    }

    private void saveBatchFailures(
            List<PythonMenuDescriptionTargetDto> batchTargets,
            String langCode,
            String reason,
            boolean retryMode,
            Map<MenuDescriptionKey, Integer> latestAttemptCounts
    ) {
        for (PythonMenuDescriptionTargetDto target : batchTargets) {
            saveDescriptionFailure(new MenuDescriptionKey(target.menuId(), langCode), reason, retryMode, latestAttemptCounts);
        }
    }

    private void saveDescriptionFailure(
            MenuDescriptionKey key,
            String reason,
            boolean retryMode,
            Map<MenuDescriptionKey, Integer> latestAttemptCounts
    ) {
        mealCrawlPersistencePort.saveMenuDescriptionAnalysis(
                key.menuId(),
                key.langCode(),
                MenuDescriptionStatus.FAILED,
                reason,
                resolveAttemptCount(retryMode, latestAttemptCounts, key)
        );
    }

    private int resolveAttemptCount(
            boolean retryMode,
            Map<MenuDescriptionKey, Integer> latestAttemptCounts,
            MenuDescriptionKey key
    ) {
        if (!retryMode) {
            return 1;
        }
        return latestAttemptCounts.getOrDefault(key, 1) + 1;
    }

    private String buildBatchFailureReason(PythonMealClientException exception) {
        String body = exception.getResponseBody();
        if (body != null && body.length() > 500) {
            body = body.substring(0, 500);
        }
        return "Python description request failed"
                + (exception.getHttpStatus() == null ? "" : " (status=" + exception.getHttpStatus() + ")")
                + (body == null || body.isBlank() ? "" : ": " + body);
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
                .map(langCode -> langCode.toLowerCase(java.util.Locale.ROOT))
                .distinct()
                .toList();
    }
}
