package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResultDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuIngredientResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MenuAiAnalysisFollowUpService {

    private static final String MENU_AI_SUCCESS = "SUCCESS";
    private static final String MENU_AI_FAILED = "FAILED";

    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final PythonMealClientPort pythonMealClientPort;

    @Transactional
    public void process(MealImportResult importResult) {
        Set<Long> targetMenuIds = new HashSet<>(importResult.menusNeedingAnalysis());
        if (targetMenuIds.isEmpty()) {
            return;
        }

        Map<Long, String> menuNames = mealCrawlPersistencePort.findMenuNamesByIds(targetMenuIds);
        List<PythonMenuAnalysisTargetDto> targets = menuNames.entrySet().stream()
                .map(entry -> new PythonMenuAnalysisTargetDto(entry.getKey(), entry.getValue()))
                .toList();

        if (targets.isEmpty()) {
            return;
        }

        PythonMenuAnalysisResponse response = pythonMealClientPort.analyzeMenus(new PythonMenuAnalysisRequest(targets));
        List<PythonMenuAnalysisResultDto> results = response.results() == null ? List.of() : response.results();

        Set<Long> handledMenuIds = new HashSet<>();
        for (PythonMenuAnalysisResultDto result : results) {
            if (result == null || result.menuId() == null || !targetMenuIds.contains(result.menuId())) {
                continue;
            }

            LocalDateTime analyzedAt = result.analyzedAt() == null ? LocalDateTime.now() : result.analyzedAt();
            String status = normalizeStatus(result.status());
            List<MenuIngredientCandidate> ingredients = toIngredients(result.ingredients());

            mealCrawlPersistencePort.saveMenuAnalysis(
                    result.menuId(),
                    status,
                    result.modelName(),
                    result.modelVersion(),
                    result.reason(),
                    analyzedAt,
                    ingredients
            );
            mealCrawlPersistencePort.updateMenuAiStatus(result.menuId(), status, analyzedAt);
            handledMenuIds.add(result.menuId());
        }

        for (Long menuId : targetMenuIds) {
            if (handledMenuIds.contains(menuId)) {
                continue;
            }
            LocalDateTime now = LocalDateTime.now();
            mealCrawlPersistencePort.saveMenuAnalysis(menuId, MENU_AI_FAILED, null, null, "No analysis response", now, List.of());
            mealCrawlPersistencePort.updateMenuAiStatus(menuId, MENU_AI_FAILED, now);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return MENU_AI_FAILED;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case MENU_AI_SUCCESS -> MENU_AI_SUCCESS;
            case MENU_AI_FAILED -> MENU_AI_FAILED;
            default -> MENU_AI_FAILED;
        };
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
}

