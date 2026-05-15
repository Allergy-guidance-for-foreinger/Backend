package com.mealguide.mealguide_api.mealcrawl.application.port;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuMatchedAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.NamedIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAllergyCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuSpicyLevel;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MealCrawlPersistencePort {

    List<CrawlTargetSource> findCrawlTargets();

    Long startCrawlHistory(Long cafeteriaId, LocalDate startDate, LocalDate endDate, LocalDateTime startedAt);

    void markCrawlHistorySuccess(Long historyId, LocalDateTime finishedAt);

    void markCrawlHistoryFailure(Long historyId, String failureMessage, LocalDateTime finishedAt);

    Long getOrCreateMealSchedule(Long cafeteriaId, LocalDate mealDate, String mealType);

    Long getOrCreateMenu(String menuName);

    void upsertMealMenu(Long mealScheduleId, Long menuId, String cornerName, int displayOrder);

    List<WeeklyMealCacheRow> findWeeklyMealsForCache(Long cafeteriaId, LocalDate weekStartDate, LocalDate weekEndDate);

    boolean existsCafeteriaInSchool(Long cafeteriaId, Long schoolId);

    Map<Long, String> findTranslatedMenuNamesByMealMenuIds(Set<Long> mealMenuIds, String langCode);

    List<MealMenuIngredientRow> findConfirmedIngredientsByMealMenuIds(Set<Long> mealMenuIds);

    Set<Long> findMealMenuIdsHavingConfirmedIngredients(Set<Long> mealMenuIds);

    List<MealMenuIngredientRow> findAiIngredientsByMealMenuIds(Set<Long> mealMenuIds);

    Set<Long> findMealMenuIdsHavingAiIngredients(Set<Long> mealMenuIds);

    List<RestrictionIngredientRow> findReligiousRestrictionIngredients(String religiousCode);

    default Optional<MenuDetailRow> findMenuDetailByMealMenuId(Long mealMenuId) {
        return Optional.empty();
    }

    default List<MenuDetailRow> findMenuDetailsByMealMenuIds(Set<Long> mealMenuIds) {
        return List.of();
    }

    default Optional<String> findTranslatedMenuNameByMealMenuId(Long mealMenuId, String langCode) {
        return Optional.empty();
    }

    default List<NamedIngredientRow> findConfirmedIngredientsForMenuDetail(Long mealMenuId, String langCode) {
        return List.of();
    }

    default List<NamedIngredientRow> findAiIngredientsForMenuDetail(Long mealMenuId, String langCode) {
        return List.of();
    }

    default List<MealMenuIngredientRow> findConfirmedIngredientsForMenuDetails(Set<Long> mealMenuIds, String langCode) {
        return List.of();
    }

    default List<MealMenuIngredientRow> findAiIngredientsForMenuDetails(Set<Long> mealMenuIds, String langCode) {
        return List.of();
    }

    default List<MealMenuMatchedAllergyRow> findMatchedAllergiesByMealMenuIds(
            Long userId,
            Set<Long> mealMenuIds,
            String langCode
    ) {
        return List.of();
    }

    default Set<Long> findMealMenuIdsHavingMatchedAllergies(Long userId, Set<Long> mealMenuIds) {
        return Set.of();
    }

    Set<Long> findAnalyzedMenuIds(Set<Long> menuIds);

    List<Long> findRetryPendingMenuIds(int limit);

    Map<Long, String> findMenuNamesByIds(Set<Long> menuIds);

    void saveMenuAnalysis(
            Long menuId,
            MenuAiStatus status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            int attemptCount,
            List<MenuIngredientCandidate> ingredients
    );

    default void saveMenuAnalysisAllergies(Long menuAiAnalysisId, List<MenuAllergyCandidate> allergies) {
    }

    default Set<String> findExistingIngredientCodes(Set<String> ingredientCodes) {
        if (ingredientCodes == null || ingredientCodes.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(ingredientCodes);
    }

    default Set<String> findExistingAllergyCodes(Set<String> allergyCodes) {
        if (allergyCodes == null || allergyCodes.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(allergyCodes);
    }

    void updateMenuAiStatus(Long menuId, MenuAiStatus aiStatus, LocalDateTime analyzedAt);

    default void updateMenuAiStatus(Long menuId, MenuAiStatus aiStatus, LocalDateTime analyzedAt, MenuSpicyLevel spicyLevel) {
        updateMenuAiStatus(menuId, aiStatus, analyzedAt);
    }

    default void saveMenuAnalysisAndUpdateStatus(
            Long menuId,
            MenuAiStatus status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            int attemptCount,
            List<MenuIngredientCandidate> ingredients
    ) {
        saveMenuAnalysis(menuId, status, modelName, modelVersion, reason, analyzedAt, attemptCount, ingredients);
        updateMenuAiStatus(menuId, status, analyzedAt);
    }

    default void saveMenuAnalysisAndUpdateStatus(
            Long menuId,
            MenuAiStatus status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            int attemptCount,
            List<MenuIngredientCandidate> ingredients,
            MenuSpicyLevel spicyLevel
    ) {
        saveMenuAnalysis(menuId, status, modelName, modelVersion, reason, analyzedAt, attemptCount, ingredients);
        updateMenuAiStatus(menuId, status, analyzedAt, spicyLevel);
    }

    default void saveMenuAnalysisAndUpdateStatus(
            Long menuId,
            MenuAiStatus status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            int attemptCount,
            List<MenuIngredientCandidate> ingredients,
            Set<String> validIngredientCodes,
            MenuSpicyLevel spicyLevel
    ) {
        Set<String> allowedCodes = validIngredientCodes == null ? Set.of() : validIngredientCodes;
        List<MenuIngredientCandidate> filteredIngredients = ingredients == null
                ? List.of()
                : ingredients.stream()
                .filter(ingredient -> ingredient != null
                        && ingredient.ingredientCode() != null
                        && !ingredient.ingredientCode().isBlank()
                        && allowedCodes.contains(ingredient.ingredientCode().trim()))
                .toList();
        saveMenuAnalysisAndUpdateStatus(
                menuId,
                status,
                modelName,
                modelVersion,
                reason,
                analyzedAt,
                attemptCount,
                filteredIngredients,
                spicyLevel
        );
    }

    default void saveMenuAnalysisAndUpdateStatus(
            Long menuId,
            MenuAiStatus status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            int attemptCount,
            List<MenuIngredientCandidate> ingredients,
            Set<String> validIngredientCodes,
            List<MenuAllergyCandidate> allergies,
            Set<String> validAllergyCodes,
            MenuSpicyLevel spicyLevel
    ) {
        throw new UnsupportedOperationException(
                "saveMenuAnalysisAndUpdateStatus with allergies must be implemented by persistence adapter"
        );
    }

    Set<MenuTranslationKey> findExistingMenuTranslationKeys(Set<Long> menuIds, List<String> langCodes);

    void saveMenuTranslation(Long menuId, String langCode, String translatedName);

    default void saveMenuTranslations(Map<MenuTranslationKey, String> translationsByKey) {
        if (translationsByKey == null || translationsByKey.isEmpty()) {
            return;
        }
        for (Map.Entry<MenuTranslationKey, String> entry : translationsByKey.entrySet()) {
            MenuTranslationKey key = entry.getKey();
            saveMenuTranslation(key.menuId(), key.langCode(), entry.getValue());
        }
    }
}

