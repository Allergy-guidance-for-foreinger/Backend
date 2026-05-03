package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MatchedAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuLikePort;
import com.mealguide.mealguide_api.review.application.port.MenuReviewPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuRiskLevel;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuDetailBatchResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuDetailQueryService {

    private static final String SOURCE_CONFIRMED = "CONFIRMED";
    private static final String SOURCE_AI = "AI";
    private static final String AI_STATUS_SUCCESS = "SUCCESS";
    private static final int MAX_BATCH_SIZE = 30;

    private final MealUserPreferencePort mealUserPreferencePort;
    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final MenuLikePort menuLikePort;
    private final MenuReviewPort menuReviewPort;

    public MenuDetailResponse getMenuDetail(Long userId, Long mealMenuId) {
        MenuDetailBatchResponse response = getMenuDetails(userId, List.of(mealMenuId));
        return response.menus().getFirst();
    }

    public MenuDetailBatchResponse getMenuDetails(Long userId, List<Long> mealMenuIds) {
        List<Long> normalizedIds = normalizeMealMenuIds(mealMenuIds);
        CurrentUserMealPreference preference = mealUserPreferencePort.getCurrentUserMealPreference(userId);
        if (preference.schoolId() == null) {
            throw new ServiceException(ErrorCode.ESSENTIAL_FIELD_MISSING_ERROR);
        }

        Set<Long> targetIds = new LinkedHashSet<>(normalizedIds);
        Map<Long, MenuDetailRow> detailsById = mealCrawlPersistencePort.findMenuDetailsByMealMenuIds(targetIds).stream()
                .collect(Collectors.toMap(
                        MenuDetailRow::mealMenuId,
                        row -> row,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
        if (detailsById.size() != targetIds.size()) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }

        boolean hasOtherSchoolMenu = detailsById.values().stream()
                .anyMatch(detail -> !preference.schoolId().equals(detail.schoolId()));
        if (hasOtherSchoolMenu) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }

        String languageCode = preference.languageCode();
        Map<Long, String> translatedMenuNames = mealCrawlPersistencePort.findTranslatedMenuNamesByMealMenuIds(targetIds, languageCode);
        Map<Long, IngredientSelection> ingredientSelections = resolveIngredients(targetIds, languageCode);

        Set<String> allIngredientCodes = ingredientSelections.values().stream()
                .flatMap(selection -> selection.ingredients().stream())
                .map(MenuDetailResponse.IngredientResponse::code)
                .collect(Collectors.toSet());

        List<MatchedAllergyRow> matchedAllergyRows = mealCrawlPersistencePort.findMatchedAllergies(userId, allIngredientCodes, languageCode);
        Map<String, List<MatchedAllergyRow>> matchedRowsByIngredientCode = matchedAllergyRows.stream()
                .collect(Collectors.groupingBy(MatchedAllergyRow::ingredientCode));

        List<RestrictionIngredientRow> religiousRestrictions =
                mealCrawlPersistencePort.findReligiousRestrictionIngredients(preference.religiousCode());
        Set<String> religiousRestrictedCodes = religiousRestrictions.stream()
                .map(RestrictionIngredientRow::ingredientCode)
                .collect(Collectors.toSet());
        Map<Long, MenuLikeTarget> likeTargetsByMealMenuId = new LinkedHashMap<>();
        for (MenuDetailRow detailRow : detailsById.values()) {
            likeTargetsByMealMenuId.put(
                    detailRow.mealMenuId(),
                    new MenuLikeTarget(detailRow.cafeteriaId(), detailRow.menuId())
            );
        }
        Set<MenuLikeTarget> likeTargets = new HashSet<>(likeTargetsByMealMenuId.values());
        Map<MenuLikeTarget, Long> likeCountByTarget = menuLikePort.countLikesByTargets(likeTargets);
        Set<MenuLikeTarget> likedTargetsByUser = menuLikePort.findLikedTargetsByUser(userId, likeTargets);
        Map<MenuLikeTarget, Long> reviewCountByTarget = menuReviewPort.countActiveReviewsByTargets(likeTargets);

        List<MenuDetailResponse> menus = new ArrayList<>(normalizedIds.size());
        for (Long mealMenuId : normalizedIds) {
            MenuDetailRow detail = detailsById.get(mealMenuId);
            IngredientSelection ingredientSelection =
                    ingredientSelections.getOrDefault(mealMenuId, new IngredientSelection(null, List.of()));

            List<MenuDetailResponse.MatchedAllergyResponse> matchedAllergies = ingredientSelection.ingredients().stream()
                    .flatMap(ingredient -> matchedRowsByIngredientCode
                            .getOrDefault(ingredient.code(), List.of())
                            .stream()
                            .map(row -> new MenuDetailResponse.MatchedAllergyResponse(
                                    row.allergyCode(),
                                    row.ingredientCode()
                            )))
                    .toList();

            MenuRiskLevel riskLevel = evaluateRiskLevel(ingredientSelection, matchedAllergies, religiousRestrictedCodes);
            String menuName = translatedMenuNames.getOrDefault(mealMenuId, detail.menuName());
            MenuLikeTarget likeTarget = likeTargetsByMealMenuId.get(mealMenuId);
            long likeCount = likeCountByTarget.getOrDefault(likeTarget, 0L);
            boolean likedByMe = likedTargetsByUser.contains(likeTarget);

            menus.add(new MenuDetailResponse(
                    detail.mealMenuId(),
                    menuName,
                    null,
                    detail.cornerName(),
                    detail.displayOrder(),
                    detail.spicyLevel(),
                    AI_STATUS_SUCCESS.equals(detail.aiAnalysisStatus()),
                    new MenuDetailResponse.MenuRiskResponse(riskLevel.name()),
                    ingredientSelection.ingredients(),
                    matchedAllergies,
                    new MenuDetailResponse.LikeResponse(likeCount, likedByMe),
                    new MenuDetailResponse.ReviewSummaryResponse(reviewCountByTarget.getOrDefault(likeTarget, 0L))
            ));
        }
        return new MenuDetailBatchResponse(menus);
    }

    private List<Long> normalizeMealMenuIds(List<Long> mealMenuIds) {
        if (mealMenuIds == null || mealMenuIds.isEmpty() || mealMenuIds.size() > MAX_BATCH_SIZE) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        LinkedHashSet<Long> deduplicated = new LinkedHashSet<>();
        for (Long mealMenuId : mealMenuIds) {
            if (mealMenuId == null || mealMenuId <= 0) {
                throw new ServiceException(ErrorCode.BINDING_ERROR);
            }
            deduplicated.add(mealMenuId);
        }
        if (deduplicated.isEmpty()) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        return List.copyOf(deduplicated);
    }

    private Map<Long, IngredientSelection> resolveIngredients(Set<Long> mealMenuIds, String languageCode) {
        Map<Long, List<MenuDetailResponse.IngredientResponse>> confirmedByMenuId = mealCrawlPersistencePort
                .findConfirmedIngredientsForMenuDetails(mealMenuIds, languageCode)
                .stream()
                .collect(Collectors.groupingBy(
                        MealMenuIngredientRow::mealMenuId,
                        Collectors.mapping(
                                row -> new MenuDetailResponse.IngredientResponse(row.ingredientCode(), SOURCE_CONFIRMED),
                                Collectors.toList()
                        )
                ));

        Map<Long, IngredientSelection> selectedByMenuId = new LinkedHashMap<>();
        Set<Long> unresolvedMenuIds = new HashSet<>();
        for (Long mealMenuId : mealMenuIds) {
            List<MenuDetailResponse.IngredientResponse> confirmed = confirmedByMenuId.getOrDefault(mealMenuId, List.of());
            if (!confirmed.isEmpty()) {
                selectedByMenuId.put(mealMenuId, new IngredientSelection(SOURCE_CONFIRMED, confirmed));
            } else {
                unresolvedMenuIds.add(mealMenuId);
            }
        }

        if (!unresolvedMenuIds.isEmpty()) {
            Map<Long, List<MenuDetailResponse.IngredientResponse>> aiByMenuId = mealCrawlPersistencePort
                    .findAiIngredientsForMenuDetails(unresolvedMenuIds, languageCode)
                    .stream()
                    .collect(Collectors.groupingBy(
                            MealMenuIngredientRow::mealMenuId,
                            Collectors.mapping(
                                    row -> new MenuDetailResponse.IngredientResponse(row.ingredientCode(), SOURCE_AI),
                                    Collectors.toList()
                            )
                    ));
            for (Long mealMenuId : unresolvedMenuIds) {
                List<MenuDetailResponse.IngredientResponse> ai = aiByMenuId.getOrDefault(mealMenuId, List.of());
                selectedByMenuId.put(mealMenuId, new IngredientSelection(ai.isEmpty() ? null : SOURCE_AI, ai));
            }
        }
        return selectedByMenuId;
    }

    private MenuRiskLevel evaluateRiskLevel(
            IngredientSelection ingredientSelection,
            List<MenuDetailResponse.MatchedAllergyResponse> matchedAllergies,
            Set<String> religiousRestrictedCodes
    ) {
        if (ingredientSelection.ingredients().isEmpty()) {
            return MenuRiskLevel.UNKNOWN;
        }
        if (!matchedAllergies.isEmpty()) {
            return MenuRiskLevel.DANGER;
        }
        boolean hasReligionRisk = ingredientSelection.ingredients().stream()
                .anyMatch(ingredient -> religiousRestrictedCodes.contains(ingredient.code()));
        if (hasReligionRisk) {
            return SOURCE_AI.equals(ingredientSelection.source()) ? MenuRiskLevel.CAUTION : MenuRiskLevel.DANGER;
        }
        return MenuRiskLevel.SAFE;
    }

    private record IngredientSelection(
            String source,
            List<MenuDetailResponse.IngredientResponse> ingredients
    ) {
    }
}
