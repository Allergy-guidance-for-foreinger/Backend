package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.IngredientTranslationTarget;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.PythonMealClientException;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonIngredientTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonIngredientTranslationTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonIngredientTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonIngredientTranslationResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientTranslationFollowUpService {

    private static final String SOURCE_LANG = "ko";
    private static final String TARGET_LANG = "en";

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final PythonMealClientPort pythonMealClientPort;
    private final MealCrawlProperties mealCrawlProperties;

    public void process(String runId) {
        Instant startedAt = Instant.now();
        int batchSize = mealCrawlProperties.getIngredientTranslationBatchSize();
        int maxBatchesPerRun = mealCrawlProperties.getIngredientTranslationMaxBatchesPerRun();
        Set<String> attemptedCodes = new HashSet<>();
        int requestedCount = 0;
        int savedCount = 0;
        int batchCount = 0;
        int skippedInvalidResultCount = 0;

        for (int batchIndex = 0; batchIndex < maxBatchesPerRun; batchIndex++) {
            List<IngredientTranslationTarget> targets = mealCrawlPersistencePort.findMissingIngredientTranslationTargets(
                    SOURCE_LANG,
                    TARGET_LANG,
                    batchSize,
                    attemptedCodes
            );
            if (targets.isEmpty()) {
                break;
            }

            Set<String> targetCodes = new HashSet<>();
            for (IngredientTranslationTarget target : targets) {
                if (target != null && !isBlank(target.ingredientCode())) {
                    targetCodes.add(target.ingredientCode().trim());
                }
            }
            attemptedCodes.addAll(targetCodes);
            requestedCount += targets.size();
            batchCount++;

            PythonIngredientTranslationResponse response;
            try {
                response = pythonMealClientPort.translateIngredients(new PythonIngredientTranslationRequest(
                        SOURCE_LANG,
                        TARGET_LANG,
                        toPythonTargets(targets)
                ));
            } catch (PythonMealClientException exception) {
                log.warn(
                        "event=FAIL stage=ingredient_translation_batch runId={} batchIndex={} batchSize={} status={} message={}",
                        runId,
                        batchIndex,
                        targets.size(),
                        exception.getHttpStatus(),
                        exception.getMessage(),
                        exception
                );
                break;
            } catch (Exception exception) {
                log.warn(
                        "event=FAIL stage=ingredient_translation_batch runId={} batchIndex={} batchSize={} message={}",
                        runId,
                        batchIndex,
                        targets.size(),
                        exception.getMessage(),
                        exception
                );
                break;
            }

            Map<String, String> translationsToSave = new LinkedHashMap<>();
            List<PythonIngredientTranslationResultDto> results = response == null || response.results() == null
                    ? List.of()
                    : response.results();
            for (PythonIngredientTranslationResultDto result : results) {
                if (result == null || isBlank(result.ingredientCode()) || isBlank(result.translatedText())) {
                    skippedInvalidResultCount++;
                    continue;
                }
                String ingredientCode = result.ingredientCode().trim();
                if (!targetCodes.contains(ingredientCode)) {
                    skippedInvalidResultCount++;
                    continue;
                }
                translationsToSave.putIfAbsent(ingredientCode, result.translatedText().trim());
            }

            mealCrawlPersistencePort.saveIngredientTranslations(TARGET_LANG, translationsToSave);
            savedCount += translationsToSave.size();
        }

        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
                "event=END stage=ingredient_translation_followup runId={} batchCount={} requestedCount={} savedCount={} skippedInvalidResultCount={} durationMs={}",
                runId,
                batchCount,
                requestedCount,
                savedCount,
                skippedInvalidResultCount,
                durationMs
        );
    }

    private List<PythonIngredientTranslationTargetDto> toPythonTargets(List<IngredientTranslationTarget> targets) {
        return targets.stream()
                .filter(target -> target != null
                        && !isBlank(target.ingredientCode())
                        && !isBlank(target.sourceName()))
                .map(target -> new PythonIngredientTranslationTargetDto(
                        target.ingredientCode().trim(),
                        target.sourceName().trim()
                ))
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
