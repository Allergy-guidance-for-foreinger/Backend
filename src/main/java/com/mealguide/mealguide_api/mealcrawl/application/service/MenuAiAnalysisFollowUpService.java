package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAllergyCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuSpicyLevel;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAllergyResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisStatus;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuIngredientResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public void process(MealImportResult importResult) {
        process("manual", null, null, null, importResult);
    }

    public void process(String runId, Long schoolId, Long cafeteriaId, LocalDate weekStartDate, MealImportResult importResult) {
        Instant startedAt = Instant.now();
        Set<Long> targetMenuIds = new HashSet<>(importResult.menusNeedingAnalysis());
        if (targetMenuIds.isEmpty()) {
            log.info(
                    "event=SKIP stage=ai_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} reason=no-target-menus",
                    runId, schoolId, cafeteriaId, weekStartDate
            );
            return;
        }

        Map<Long, String> menuNames = mealCrawlPersistencePort.findMenuNamesByIds(targetMenuIds);
        List<PythonMenuAnalysisTargetDto> targets = menuNames.entrySet().stream()
                .map(entry -> new PythonMenuAnalysisTargetDto(entry.getKey(), entry.getValue()))
                .toList();

        if (targets.isEmpty()) {
            log.info(
                    "event=SKIP stage=ai_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} reason=no-analysis-targets targetMenuCount={} menuNameCount={}",
                    runId,
                    schoolId,
                    cafeteriaId,
                    weekStartDate,
                    targetMenuIds.size(),
                    menuNames.size()
            );
            return;
        }

        try {
            log.info(
                    "event=START stage=ai_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} targetMenuCount={} requestTargetCount={}",
                    runId,
                    schoolId,
                    cafeteriaId,
                    weekStartDate,
                    targetMenuIds.size(),
                    targets.size()
            );
            PythonMenuAnalysisResponse response = pythonMealClientPort.analyzeMenus(
                    new PythonMenuAnalysisRequest(targets, true, true)
            );
            List<PythonMenuAnalysisResultDto> results = response.results() == null ? List.of() : response.results();
            log.info(
                    "event=INFO stage=ai_followup_response runId={} schoolId={} cafeteriaId={} weekStartDate={} resultCount={}",
                    runId, schoolId, cafeteriaId, weekStartDate, results.size()
            );

            Set<Long> handledMenuIds = new HashSet<>();
            Set<String> candidateIngredientCodes = extractCandidateIngredientCodes(results, targetMenuIds);
            Set<String> validIngredientCodes = mealCrawlPersistencePort.findExistingIngredientCodes(candidateIngredientCodes);
            Set<String> candidateAllergyCodes = extractCandidateAllergyCodes(results, targetMenuIds);
            Set<String> validAllergyCodes = mealCrawlPersistencePort.findExistingAllergyCodes(candidateAllergyCodes);
            int successCount = 0;
            int failedCount = 0;
            for (PythonMenuAnalysisResultDto result : results) {
                if (result == null || result.menuId() == null || !targetMenuIds.contains(result.menuId())) {
                    continue;
                }

                LocalDateTime analyzedAt = LocalDateTime.now();
                MenuAiStatus status = normalizeStatus(result);
                MenuSpicyLevel spicyLevel = normalizeSpicyLevel(result.spicyLevel());
                List<MenuIngredientCandidate> ingredients = toIngredients(result.ingredients());
                List<MenuAllergyCandidate> allergies = toAllergies(result.allergies());

                mealCrawlPersistencePort.saveMenuAnalysisAndUpdateStatus(
                        result.menuId(),
                        status,
                        result.modelName(),
                        result.modelVersion(),
                        result.reason(),
                        analyzedAt,
                        ingredients,
                        validIngredientCodes,
                        allergies,
                        validAllergyCodes,
                        spicyLevel
                );
                if (status == MenuAiStatus.SUCCESS) {
                    successCount++;
                } else {
                    failedCount++;
                }
                handledMenuIds.add(result.menuId());
            }

            for (Long menuId : targetMenuIds) {
                if (handledMenuIds.contains(menuId)) {
                    continue;
                }
                LocalDateTime now = LocalDateTime.now();
                mealCrawlPersistencePort.saveMenuAnalysisAndUpdateStatus(
                        menuId,
                        MenuAiStatus.FAILED,
                        null,
                        null,
                        "No analysis response",
                        now,
                        List.of(),
                        Set.of(),
                        List.of(),
                        Set.of(),
                        null
                );
                failedCount++;
            }
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.info(
                    "event=END stage=ai_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} handledCount={} markedFailedCount={} successCount={} failedCount={} durationMs={} result={}",
                    runId,
                    schoolId,
                    cafeteriaId,
                    weekStartDate,
                    handledMenuIds.size(),
                    targetMenuIds.size() - handledMenuIds.size(),
                    successCount,
                    failedCount,
                    durationMs,
                    failedCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS"
            );
        } catch (Exception exception) {
            markMenusAsFailed(targetMenuIds, "AI follow-up failed");
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.warn(
                    "event=FAIL stage=ai_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} targetMenuCount={} durationMs={} errorType={} message={}",
                    runId,
                    schoolId,
                    cafeteriaId,
                    weekStartDate,
                    targetMenuIds.size(),
                    durationMs,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
            throw exception;
        }
    }

    private void markMenusAsFailed(Set<Long> targetMenuIds, String reason) {
        LocalDateTime failedAt = LocalDateTime.now();
        for (Long menuId : targetMenuIds) {
            try {
                mealCrawlPersistencePort.saveMenuAnalysisAndUpdateStatus(
                        menuId,
                        MenuAiStatus.FAILED,
                        null,
                        null,
                        reason,
                        failedAt,
                        List.of(),
                        Set.of(),
                        List.of(),
                        Set.of(),
                        null
                );
            } catch (Exception updateException) {
                log.warn("Failed to mark menu AI status as FAILED: menuId={}", menuId, updateException);
            }
        }
    }

    private MenuAiStatus normalizeStatus(PythonMenuAnalysisResultDto result) {
        PythonMenuAnalysisStatus status = result.status();
        if (status == null) {
            return inferStatusWithoutExplicitValue(result);
        }
        return switch (status) {
            case SUCCESS -> MenuAiStatus.SUCCESS;
            case FAILED -> MenuAiStatus.FAILED;
        };
    }

    private MenuAiStatus inferStatusWithoutExplicitValue(PythonMenuAnalysisResultDto result) {
        boolean hasIngredients = result.ingredients() != null
                && result.ingredients().stream()
                .anyMatch(ingredient -> ingredient != null
                        && ingredient.ingredientCode() != null
                        && !ingredient.ingredientCode().isBlank());
        boolean hasFailureReason = result.reason() != null && !result.reason().isBlank();
        return hasIngredients && !hasFailureReason ? MenuAiStatus.SUCCESS : MenuAiStatus.FAILED;
    }

    private List<MenuIngredientCandidate> toIngredients(List<PythonMenuIngredientResultDto> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return List.of();
        }

        return ingredients.stream()
                .filter(ingredient -> ingredient != null && ingredient.ingredientCode() != null && !ingredient.ingredientCode().isBlank())
                .map(ingredient -> new MenuIngredientCandidate(ingredient.ingredientCode().trim(), ingredient.confidence()))
                .toList();
    }

    private MenuSpicyLevel normalizeSpicyLevel(Long spicyLevel) {
        return MenuSpicyLevel.fromValue(spicyLevel);
    }

    private List<MenuAllergyCandidate> toAllergies(List<PythonMenuAllergyResultDto> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return List.of();
        }
        return allergies.stream()
                .filter(allergy -> allergy != null && allergy.allergyCode() != null && !allergy.allergyCode().isBlank())
                .map(allergy -> new MenuAllergyCandidate(
                        allergy.allergyCode().trim(),
                        allergy.confidence(),
                        null
                ))
                .toList();
    }

    private Set<String> extractCandidateIngredientCodes(
            List<PythonMenuAnalysisResultDto> results,
            Set<Long> targetMenuIds
    ) {
        if (results == null || results.isEmpty()) {
            return Set.of();
        }
        Set<String> candidateCodes = new HashSet<>();
        for (PythonMenuAnalysisResultDto result : results) {
            if (result == null || result.menuId() == null || !targetMenuIds.contains(result.menuId())) {
                continue;
            }
            for (MenuIngredientCandidate ingredient : toIngredients(result.ingredients())) {
                candidateCodes.add(ingredient.ingredientCode());
            }
        }
        return candidateCodes;
    }

    private Set<String> extractCandidateAllergyCodes(
            List<PythonMenuAnalysisResultDto> results,
            Set<Long> targetMenuIds
    ) {
        if (results == null || results.isEmpty()) {
            return Set.of();
        }
        Set<String> candidateCodes = new HashSet<>();
        for (PythonMenuAnalysisResultDto result : results) {
            if (result == null || result.menuId() == null || !targetMenuIds.contains(result.menuId())) {
                continue;
            }
            for (MenuAllergyCandidate allergy : toAllergies(result.allergies())) {
                candidateCodes.add(allergy.allergyCode());
            }
        }
        return candidateCodes;
    }
}

