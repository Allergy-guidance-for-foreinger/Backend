package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MatchedAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.NamedIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuRiskLevel;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuDetailQueryService {

    private static final String LANG_KO = "ko";
    private static final String SOURCE_CONFIRMED = "CONFIRMED";
    private static final String SOURCE_AI = "AI";
    private static final String AI_STATUS_SUCCESS = "SUCCESS";

    private final MealUserPreferencePort mealUserPreferencePort;
    private final MealCrawlPersistencePort mealCrawlPersistencePort;

    public MenuDetailResponse getMenuDetail(Long userId, Long mealMenuId) {
        CurrentUserMealPreference preference = mealUserPreferencePort.getCurrentUserMealPreference(userId);
        if (preference.schoolId() == null) {
            throw new ServiceException(ErrorCode.ESSENTIAL_FIELD_MISSING_ERROR);
        }

        MenuDetailRow detail = mealCrawlPersistencePort.findMenuDetailByMealMenuId(mealMenuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));

        if (!preference.schoolId().equals(detail.schoolId())) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }

        String menuName = mealCrawlPersistencePort.findTranslatedMenuNameByMealMenuId(mealMenuId, preference.languageCode())
                .orElse(detail.menuName());

        IngredientSelection ingredientSelection = resolveIngredients(mealMenuId, preference.languageCode());
        Set<String> ingredientCodes = extractIngredientCodes(ingredientSelection.ingredients());
        List<MatchedAllergyRow> matchedAllergyRows = mealCrawlPersistencePort.findMatchedAllergies(
                userId,
                ingredientCodes,
                preference.languageCode()
        );

        List<MenuDetailResponse.MatchedAllergyResponse> matchedAllergies = matchedAllergyRows.stream()
                .map(row -> new MenuDetailResponse.MatchedAllergyResponse(
                        row.allergyCode(),
                        row.allergyName(),
                        row.ingredientCode(),
                        row.ingredientName(),
                        buildMatchedMessage(preference.languageCode(), row.ingredientName())
                ))
                .toList();

        MenuRiskLevel riskLevel = evaluateRiskLevel(
                ingredientSelection,
                matchedAllergies,
                preference.religiousCode()
        );

        return new MenuDetailResponse(
                detail.mealMenuId(),
                menuName,
                null,
                detail.cornerName(),
                detail.displayOrder(),
                detail.spicyLevel(),
                AI_STATUS_SUCCESS.equals(detail.aiAnalysisStatus()),
                new MenuDetailResponse.MenuRiskResponse(riskLevel.name()),
                ingredientSelection.ingredients().stream()
                        .map(ingredient -> new MenuDetailResponse.IngredientResponse(
                                ingredient.code(),
                                ingredient.name(),
                                ingredientSelection.source()
                        ))
                        .toList(),
                matchedAllergies
        );
    }

    private IngredientSelection resolveIngredients(Long mealMenuId, String languageCode) {
        List<NamedIngredientRow> confirmedIngredients = mealCrawlPersistencePort.findConfirmedIngredientsForMenuDetail(mealMenuId, languageCode);
        if (!confirmedIngredients.isEmpty()) {
            return new IngredientSelection(SOURCE_CONFIRMED, confirmedIngredients);
        }

        List<NamedIngredientRow> aiIngredients = mealCrawlPersistencePort.findAiIngredientsForMenuDetail(mealMenuId, languageCode);
        if (!aiIngredients.isEmpty()) {
            return new IngredientSelection(SOURCE_AI, aiIngredients);
        }

        return new IngredientSelection(null, List.of());
    }

    private Set<String> extractIngredientCodes(List<NamedIngredientRow> ingredients) {
        return ingredients.stream()
                .map(NamedIngredientRow::code)
                .collect(Collectors.toSet());
    }

    private MenuRiskLevel evaluateRiskLevel(
            IngredientSelection ingredientSelection,
            List<MenuDetailResponse.MatchedAllergyResponse> matchedAllergies,
            String religiousCode
    ) {
        if (ingredientSelection.ingredients().isEmpty()) {
            return MenuRiskLevel.UNKNOWN;
        }
        if (!matchedAllergies.isEmpty()) {
            return MenuRiskLevel.DANGER;
        }

        List<RestrictionIngredientRow> religiousRestrictions = mealCrawlPersistencePort.findReligiousRestrictionIngredients(religiousCode);
        Set<String> restrictedCodes = religiousRestrictions.stream()
                .map(RestrictionIngredientRow::ingredientCode)
                .collect(Collectors.toSet());
        
        boolean hasReligionRisk = ingredientSelection.ingredients().stream()
                .anyMatch(ingredient -> restrictedCodes.contains(ingredient.code()));
        if (hasReligionRisk) {
            if (SOURCE_AI.equals(ingredientSelection.source())) {
                return MenuRiskLevel.CAUTION;
            }
            return MenuRiskLevel.DANGER;
        }
        return MenuRiskLevel.SAFE;
    }

    private String buildMatchedMessage(String languageCode, String ingredientName) {
        String normalizedLanguageCode = languageCode == null ? "" : languageCode.trim().toLowerCase(Locale.ROOT);
        if (LANG_KO.equals(normalizedLanguageCode)) {
            return "내 알러지와 겹치는 식재료: " + ingredientName;
        }
        return "Ingredient matching my allergy: " + ingredientName;
    }

    private record IngredientSelection(
            String source,
            List<NamedIngredientRow> ingredients
    ) {
    }
}
