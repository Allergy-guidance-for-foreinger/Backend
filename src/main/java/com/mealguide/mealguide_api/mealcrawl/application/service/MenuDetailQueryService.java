package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.CurrentUserMealPreference;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuMatchedAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuReligiousMatchRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealUserPreferencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuLikePort;
import com.mealguide.mealguide_api.review.application.port.MenuReviewPort;
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

        Map<Long, List<MealMenuMatchedAllergyRow>> matchedRowsByMealMenuId = mealCrawlPersistencePort
                .findMatchedAllergiesByMealMenuIds(userId, targetIds, languageCode)
                .stream()
                .collect(Collectors.groupingBy(MealMenuMatchedAllergyRow::mealMenuId));
        Map<Long, List<MealMenuAllergyRow>> allergiesByMealMenuId = mealCrawlPersistencePort
                .findAllergiesByMealMenuIds(targetIds, languageCode)
                .stream()
                .collect(Collectors.groupingBy(MealMenuAllergyRow::mealMenuId));
        Map<Long, List<MealMenuReligiousMatchRow>> religiousMatchesByMealMenuId = mealCrawlPersistencePort
                .findReligiousMatchedIngredientsByMealMenuIds(targetIds, preference.religiousCode(), languageCode)
                .stream()
                .collect(Collectors.groupingBy(MealMenuReligiousMatchRow::mealMenuId));
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

            List<MenuDetailResponse.MatchedAllergyResponse> matchedAllergies = matchedRowsByMealMenuId
                    .getOrDefault(mealMenuId, List.of())
                    .stream()
                    .map(row -> new MenuDetailResponse.MatchedAllergyResponse(
                            row.allergyCode(),
                            row.allergyName(),
                            "DANGER",
                            row.confidence()
                    ))
                    .toList();
            List<MenuDetailResponse.AllergyResponse> allergies = allergiesByMealMenuId
                    .getOrDefault(mealMenuId, List.of())
                    .stream()
                    .map(row -> new MenuDetailResponse.AllergyResponse(
                            row.allergyCode(),
                            row.allergyName(),
                            SOURCE_AI
                    ))
                    .toList();
            List<MenuDetailResponse.MatchedReligiousIngredientResponse> matchedReligiousIngredients =
                    mapMatchedReligiousIngredients(
                            ingredientSelection,
                            religiousMatchesByMealMenuId.getOrDefault(mealMenuId, List.of())
                    );
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
                    allergies,
                    matchedAllergies,
                    ingredientSelection.ingredients(),
                    matchedReligiousIngredients,
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
                                row -> new MenuDetailResponse.IngredientResponse(
                                        row.ingredientCode(),
                                        row.ingredientName(),
                                        SOURCE_CONFIRMED
                                ),
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
                                    row -> new MenuDetailResponse.IngredientResponse(
                                            row.ingredientCode(),
                                            row.ingredientName(),
                                            SOURCE_AI
                                    ),
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

    private List<MenuDetailResponse.MatchedReligiousIngredientResponse> mapMatchedReligiousIngredients(
            IngredientSelection ingredientSelection,
            List<MealMenuReligiousMatchRow> religiousMatchRows
    ) {
        if (religiousMatchRows.isEmpty() || ingredientSelection.ingredients().isEmpty()) {
            return List.of();
        }
        String riskLevel = SOURCE_AI.equals(ingredientSelection.source()) ? "CAUTION" : "DANGER";
        Map<String, List<MealMenuReligiousMatchRow>> byIngredientCode = religiousMatchRows.stream()
                .collect(Collectors.groupingBy(MealMenuReligiousMatchRow::ingredientCode));
        return ingredientSelection.ingredients().stream()
                .filter(ingredient -> byIngredientCode.containsKey(ingredient.code()))
                .map(ingredient -> {
                    List<MenuDetailResponse.MatchedReligiousRestrictionResponse> matchedRestrictions =
                            byIngredientCode.getOrDefault(ingredient.code(), List.of())
                                    .stream()
                                    .map(row -> new MenuDetailResponse.MatchedReligiousRestrictionResponse(
                                            row.restrictionCode(),
                                            row.restrictionName(),
                                            riskLevel
                                    ))
                                    .toList();
                    return new MenuDetailResponse.MatchedReligiousIngredientResponse(
                            ingredient.code(),
                            ingredient.name(),
                            SOURCE_AI.equals(ingredientSelection.source())
                                    ? byIngredientCode.getOrDefault(ingredient.code(), List.of()).stream()
                                    .map(MealMenuReligiousMatchRow::confidence)
                                    .filter(java.util.Objects::nonNull)
                                    .findFirst()
                                    .orElse(null)
                                    : null,
                            matchedRestrictions
                    );
                })
                .toList();
    }

    private record IngredientSelection(
            String source,
            List<MenuDetailResponse.IngredientResponse> ingredients
    ) {
    }
}
