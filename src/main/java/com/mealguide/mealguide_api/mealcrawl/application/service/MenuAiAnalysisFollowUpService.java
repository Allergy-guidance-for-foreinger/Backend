package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisStatus;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuIngredientResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        Set<Long> targetMenuIds = new HashSet<>(importResult.menusNeedingAnalysis());
        if (targetMenuIds.isEmpty()) {
            log.info("Menu AI analysis follow-up skipped: reason=no-target-menus");
            return;
        }

        Map<Long, String> menuNames = mealCrawlPersistencePort.findMenuNamesByIds(targetMenuIds);
        List<PythonMenuAnalysisTargetDto> targets = menuNames.entrySet().stream()
                .map(entry -> new PythonMenuAnalysisTargetDto(entry.getKey(), entry.getValue()))
                .toList();

        if (targets.isEmpty()) {
            log.info(
                    "Menu AI analysis follow-up skipped: reason=no-analysis-targets, targetMenuCount={}, menuNameCount={}",
                    targetMenuIds.size(),
                    menuNames.size()
            );
            return;
        }

        try {
            log.info(
                    "Menu AI analysis follow-up started: targetMenuCount={}, requestTargetCount={}",
                    targetMenuIds.size(),
                    targets.size()
            );
            PythonMenuAnalysisResponse response = pythonMealClientPort.analyzeMenus(new PythonMenuAnalysisRequest(targets));
            List<PythonMenuAnalysisResultDto> results = response.results() == null ? List.of() : response.results();
            log.info("Menu AI analysis follow-up response received: resultCount={}", results.size());

            Set<Long> handledMenuIds = new HashSet<>();
            Set<String> candidateIngredientCodes = extractCandidateIngredientCodes(results, targetMenuIds);
            Set<String> validIngredientCodes = mealCrawlPersistencePort.findExistingIngredientCodes(candidateIngredientCodes);
            for (PythonMenuAnalysisResultDto result : results) {
                if (result == null || result.menuId() == null || !targetMenuIds.contains(result.menuId())) {
                    continue;
                }

                LocalDateTime analyzedAt = result.analyzedAt() == null ? LocalDateTime.now() : result.analyzedAt();
                MenuAiStatus status = normalizeStatus(result);
                List<MenuIngredientCandidate> ingredients = toIngredients(result.ingredients());

                mealCrawlPersistencePort.saveMenuAnalysisAndUpdateStatus(
                        result.menuId(),
                        status,
                        result.modelName(),
                        result.modelVersion(),
                        result.reason(),
                        analyzedAt,
                        ingredients,
                        validIngredientCodes
                );
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
                        List.of()
                );
            }
            log.info(
                    "Menu AI analysis follow-up completed: handledCount={}, markedFailedCount={}",
                    handledMenuIds.size(),
                    targetMenuIds.size() - handledMenuIds.size()
            );
        } catch (Exception exception) {
            markMenusAsFailed(targetMenuIds, "AI follow-up failed");
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
                        List.of()
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
}

