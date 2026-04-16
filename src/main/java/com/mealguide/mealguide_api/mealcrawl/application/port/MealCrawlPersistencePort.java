package com.mealguide.mealguide_api.mealcrawl.application.port;

import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MealCrawlPersistencePort {

    List<CrawlTargetSource> findCrawlTargets();

    Long startCrawlHistory(Long cafeteriaId, LocalDate startDate, LocalDate endDate, LocalDateTime startedAt);

    void markCrawlHistorySuccess(Long historyId, LocalDateTime finishedAt);

    void markCrawlHistoryFailure(Long historyId, String failureMessage, LocalDateTime finishedAt);

    Long getOrCreateMealSchedule(Long cafeteriaId, LocalDate mealDate, String mealType);

    Long getOrCreateMenu(String menuName);

    void upsertMealMenu(Long mealScheduleId, Long menuId, String cornerName, int displayOrder);

    Set<Long> findAnalyzedMenuIds(Set<Long> menuIds);

    Map<Long, String> findMenuNamesByIds(Set<Long> menuIds);

    void saveMenuAnalysis(
            Long menuId,
            String status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            List<MenuIngredientCandidate> ingredients
    );

    void updateMenuAiStatus(Long menuId, String aiStatus, LocalDateTime analyzedAt);

    Set<MenuTranslationKey> findExistingMenuTranslationKeys(Set<Long> menuIds, List<String> langCodes);

    void saveMenuTranslation(Long menuId, String langCode, String translatedName);
}

