package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAllergyCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuSpicyLevel;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.PythonMealClientException;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAllergyResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisStatus;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuIngredientResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuAiAnalysisFollowUpService {

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final PythonMealClientPort pythonMealClientPort;
    private final MealCrawlProperties mealCrawlProperties;

    public void process(MealImportResult importResult) {
        process("manual", null, null, null, importResult);
    }

    public void process(String runId, Long schoolId, Long cafeteriaId, LocalDate weekStartDate, MealImportResult importResult) {
        Set<Long> targetMenuIds = new HashSet<>(importResult.menusNeedingAnalysis());
        processMenuIds(runId, schoolId, cafeteriaId, weekStartDate, targetMenuIds, false);
    }

    public void processRetryPending(String runId) {
        List<Long> retryTargetMenuIds = mealCrawlPersistencePort.findRetryTargetMenuIds(
                mealCrawlProperties.getAiAnalysisRetryBatchSize(),
                mealCrawlProperties.getAiAnalysisMaxAttemptCount()
        );
        processMenuIds(runId, null, null, null, new HashSet<>(retryTargetMenuIds), true);
    }

    private void processMenuIds(
            String runId,
            Long schoolId,
            Long cafeteriaId,
            LocalDate weekStartDate,
            Set<Long> targetMenuIds,
            boolean retryMode
    ) {
        if (targetMenuIds.isEmpty()) {
            log.info("event=SKIP stage=ai_followup runId={} retryMode={} reason=no-target-menus", runId, retryMode);
            return;
        }

        Map<Long, String> menuNames = mealCrawlPersistencePort.findMenuNamesByIds(targetMenuIds);
        List<PythonMenuAnalysisTargetDto> targets = menuNames.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> new PythonMenuAnalysisTargetDto(entry.getKey(), entry.getValue()))
                .toList();
        if (targets.isEmpty()) {
            log.info("event=SKIP stage=ai_followup runId={} retryMode={} reason=no-analysis-targets", runId, retryMode);
            return;
        }

        int batchSize = retryMode ? mealCrawlProperties.getAiAnalysisRetryBatchSize() : mealCrawlProperties.getAiAnalysisBatchSize();
        for (int start = 0; start < targets.size(); start += batchSize) {
            int end = Math.min(start + batchSize, targets.size());
            processBatch(runId, schoolId, cafeteriaId, weekStartDate, targets.subList(start, end), retryMode);
        }
    }

    private void processBatch(
            String runId,
            Long schoolId,
            Long cafeteriaId,
            LocalDate weekStartDate,
            List<PythonMenuAnalysisTargetDto> batchTargets,
            boolean retryMode
    ) {
        Set<Long> batchMenuIds = batchTargets.stream().map(PythonMenuAnalysisTargetDto::menuId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Integer> latestAttemptCountByMenuId = retryMode
                ? mealCrawlPersistencePort.findLatestAttemptCounts(batchMenuIds)
                : Map.of();
        try {
            PythonMenuAnalysisResponse response = pythonMealClientPort.analyzeMenus(new PythonMenuAnalysisRequest(batchTargets, true, true));
            if (response == null) {
                for (PythonMenuAnalysisTargetDto target : batchTargets) {
                    saveFailure(
                            target.menuId(),
                            MenuAiStatus.FAILED,
                            "AI analysis response is null",
                            resolveAttemptCount(retryMode, latestAttemptCountByMenuId, target.menuId())
                    );
                }
                log.warn(
                        "event=FAIL stage=ai_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} batchSize={} message=null-analysis-response",
                        runId, schoolId, cafeteriaId, weekStartDate, retryMode, batchTargets.size()
                );
                return;
            }
            List<PythonMenuAnalysisResultDto> results = response.results() == null ? List.of() : response.results();
            Map<Long, PythonMenuAnalysisResultDto> resultsByMenuId = new HashMap<>();
            for (PythonMenuAnalysisResultDto result : results) {
                if (result == null || result.menuId() == null || !batchMenuIds.contains(result.menuId())) {
                    continue;
                }
                resultsByMenuId.putIfAbsent(result.menuId(), result);
            }

            Set<String> validIngredientCodes = mealCrawlPersistencePort.findExistingIngredientCodes(
                    extractCandidateIngredientCodes(resultsByMenuId.values())
            );
            Set<String> validAllergyCodes = mealCrawlPersistencePort.findExistingAllergyCodes(
                    extractCandidateAllergyCodes(resultsByMenuId.values())
            );
            LocalDateTime analyzedAt = LocalDateTime.now();

            for (PythonMenuAnalysisTargetDto target : batchTargets) {
                PythonMenuAnalysisResultDto result = resultsByMenuId.get(target.menuId());
                if (result == null) {
                    saveFailure(
                            target.menuId(),
                            MenuAiStatus.FAILED,
                            "No analysis response",
                            resolveAttemptCount(retryMode, latestAttemptCountByMenuId, target.menuId())
                    );
                    continue;
                }

                MenuAiStatus status = mapToMenuAiStatus(result, retryMode);
                mealCrawlPersistencePort.saveMenuAnalysisAndUpdateStatus(
                        result.menuId(),
                        status,
                        result.modelName(),
                        result.modelVersion(),
                        result.reason(),
                        analyzedAt,
                        resolveAttemptCount(retryMode, latestAttemptCountByMenuId, result.menuId()),
                        toIngredients(result.ingredients()),
                        validIngredientCodes,
                        toAllergies(result.allergies()),
                        validAllergyCodes,
                        normalizeSpicyLevel(result.spicyLevel())
                );
            }
        } catch (PythonMealClientException exception) {
            MenuAiStatus status = MenuAiStatus.FAILED;
            String message = buildBatchFailureReason(exception);
            for (PythonMenuAnalysisTargetDto target : batchTargets) {
                saveFailure(target.menuId(), status, message, resolveAttemptCount(retryMode, latestAttemptCountByMenuId, target.menuId()));
            }
            log.warn(
                    "event=FAIL stage=ai_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} batchSize={} status={} message={}",
                    runId, schoolId, cafeteriaId, weekStartDate, retryMode, batchTargets.size(), exception.getHttpStatus(), exception.getMessage(), exception
            );
        } catch (Exception exception) {
            MenuAiStatus status = MenuAiStatus.FAILED;
            for (PythonMenuAnalysisTargetDto target : batchTargets) {
                saveFailure(target.menuId(), status, "AI follow-up batch failed", resolveAttemptCount(retryMode, latestAttemptCountByMenuId, target.menuId()));
            }
            log.warn(
                    "event=FAIL stage=ai_followup_batch runId={} schoolId={} cafeteriaId={} weekStartDate={} retryMode={} batchSize={} message={}",
                    runId, schoolId, cafeteriaId, weekStartDate, retryMode, batchTargets.size(), exception.getMessage(), exception
            );
        }
    }

    private void saveFailure(Long menuId, MenuAiStatus status, String reason, int attemptCount) {
        mealCrawlPersistencePort.saveMenuAnalysisAndUpdateStatus(
                menuId,
                status,
                null,
                null,
                reason,
                LocalDateTime.now(),
                attemptCount,
                List.of(),
                Set.of(),
                List.of(),
                Set.of(),
                null
        );
    }

    private MenuAiStatus mapToMenuAiStatus(PythonMenuAnalysisResultDto result, boolean retryMode) {
        PythonMenuAnalysisStatus status = result.status();
        if (status == null) {
            return inferStatusWithoutExplicitValue(result, retryMode);
        }
        return switch (status) {
            case SUCCESS -> MenuAiStatus.SUCCESS;
            case RETRYABLE_FAILED, PERMANENT_FAILED -> MenuAiStatus.FAILED;
        };
    }

    private MenuAiStatus inferStatusWithoutExplicitValue(PythonMenuAnalysisResultDto result, boolean retryMode) {
        boolean hasIngredients = result.ingredients() != null
                && result.ingredients().stream().anyMatch(ingredient ->
                ingredient != null && ingredient.ingredientCode() != null && !ingredient.ingredientCode().isBlank()
        );
        boolean hasFailureReason = result.reason() != null && !result.reason().isBlank();
        if (hasIngredients && !hasFailureReason) {
            return MenuAiStatus.SUCCESS;
        }
        return MenuAiStatus.FAILED;
    }

    private int resolveAttemptCount(boolean retryMode, Map<Long, Integer> latestAttemptCountByMenuId, Long menuId) {
        if (!retryMode) {
            return 1;
        }
        return latestAttemptCountByMenuId.getOrDefault(menuId, 1) + 1;
    }

    private List<MenuIngredientCandidate> toIngredients(List<PythonMenuIngredientResultDto> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return List.of();
        }
        List<MenuIngredientCandidate> converted = new ArrayList<>();
        for (PythonMenuIngredientResultDto ingredient : ingredients) {
            if (ingredient == null
                    || (isBlank(ingredient.ingredientCode()) && isBlank(ingredient.ingredientName()))) {
                continue;
            }
            converted.add(new MenuIngredientCandidate(
                    trimToNull(ingredient.ingredientCode()),
                    trimToNull(ingredient.ingredientName()),
                    ingredient.confidence()
            ));
        }
        return converted;
    }

    private List<MenuAllergyCandidate> toAllergies(List<PythonMenuAllergyResultDto> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return List.of();
        }
        List<MenuAllergyCandidate> converted = new ArrayList<>();
        for (PythonMenuAllergyResultDto allergy : allergies) {
            if (allergy == null || allergy.allergyCode() == null || allergy.allergyCode().isBlank()) {
                continue;
            }
            converted.add(new MenuAllergyCandidate(allergy.allergyCode().trim(), allergy.confidence(), null));
        }
        return converted;
    }

    private Set<String> extractCandidateIngredientCodes(java.util.Collection<PythonMenuAnalysisResultDto> results) {
        Set<String> codes = new HashSet<>();
        for (PythonMenuAnalysisResultDto result : results) {
            for (MenuIngredientCandidate ingredient : toIngredients(result.ingredients())) {
                if (!isBlank(ingredient.ingredientCode())) {
                    codes.add(ingredient.ingredientCode().trim());
                }
            }
        }
        return codes;
    }

    private Set<String> extractCandidateAllergyCodes(java.util.Collection<PythonMenuAnalysisResultDto> results) {
        Set<String> codes = new HashSet<>();
        for (PythonMenuAnalysisResultDto result : results) {
            for (MenuAllergyCandidate allergy : toAllergies(result.allergies())) {
                codes.add(allergy.allergyCode());
            }
        }
        return codes;
    }

    private MenuSpicyLevel normalizeSpicyLevel(Long spicyLevel) {
        return MenuSpicyLevel.fromValue(spicyLevel);
    }

    private String buildBatchFailureReason(PythonMealClientException exception) {
        String body = exception.getResponseBody();
        if (body != null && body.length() > 500) {
            body = body.substring(0, 500);
        }
        return "Python analysis request failed"
                + (exception.getHttpStatus() == null ? "" : " (status=" + exception.getHttpStatus() + ")")
                + (body == null || body.isBlank() ? "" : ": " + body);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
