package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MealMenu;
import com.mealguide.mealguide_api.mealcrawl.domain.MealSchedule;
import com.mealguide.mealguide_api.mealcrawl.domain.MealScheduleCrawlHistory;
import com.mealguide.mealguide_api.mealcrawl.domain.Menu;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysis;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisIngredient;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslation;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.CafeteriaJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MealMenuJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MealScheduleCrawlHistoryJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MealScheduleJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuAiAnalysisIngredientJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuAiAnalysisJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuTranslationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MealCrawlPersistenceAdapter implements MealCrawlPersistencePort {

    private static final String DEFAULT_MENU_AI_STATUS = "PENDING";
    private static final String INGREDIENT_SOURCE_TYPE_CRAWL = "CRAWL";
    private static final String INGREDIENT_STATUS_PENDING = "PENDING";

    private final CafeteriaJpaRepository cafeteriaJpaRepository;
    private final MealScheduleCrawlHistoryJpaRepository crawlHistoryJpaRepository;
    private final MealScheduleJpaRepository mealScheduleJpaRepository;
    private final MenuJpaRepository menuJpaRepository;
    private final MealMenuJpaRepository mealMenuJpaRepository;
    private final MenuAiAnalysisJpaRepository menuAiAnalysisJpaRepository;
    private final MenuAiAnalysisIngredientJpaRepository menuAiAnalysisIngredientJpaRepository;
    private final MenuTranslationJpaRepository menuTranslationJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CrawlTargetSource> findCrawlTargets() {
        return cafeteriaJpaRepository.findAllCrawlTargets();
    }

    @Override
    @Transactional
    public Long startCrawlHistory(Long cafeteriaId, LocalDate startDate, LocalDate endDate, LocalDateTime startedAt) {
        MealScheduleCrawlHistory history = MealScheduleCrawlHistory.start(cafeteriaId, startDate, endDate, startedAt);
        return crawlHistoryJpaRepository.save(history).getId();
    }

    @Override
    @Transactional
    public void markCrawlHistorySuccess(Long historyId, LocalDateTime finishedAt) {
        MealScheduleCrawlHistory history = crawlHistoryJpaRepository.findById(historyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        history.markSuccess(finishedAt);
    }

    @Override
    @Transactional
    public void markCrawlHistoryFailure(Long historyId, String failureMessage, LocalDateTime finishedAt) {
        MealScheduleCrawlHistory history = crawlHistoryJpaRepository.findById(historyId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        history.markFailed(failureMessage, finishedAt);
    }

    @Override
    @Transactional
    public Long getOrCreateMealSchedule(Long cafeteriaId, LocalDate mealDate, String mealType) {
        return mealScheduleJpaRepository.findByCafeteriaIdAndMealDateAndMealType(cafeteriaId, mealDate, mealType)
                .map(MealSchedule::getId)
                .orElseGet(() -> mealScheduleJpaRepository.save(
                        MealSchedule.create(cafeteriaId, mealDate, mealType)
                ).getId());
    }

    @Override
    @Transactional
    public Long getOrCreateMenu(String menuName) {
        String normalizedMenuName = menuName.trim();
        return menuJpaRepository.findFirstByName(normalizedMenuName)
                .map(Menu::getId)
                .orElseGet(() -> menuJpaRepository.save(
                        Menu.create(normalizedMenuName, DEFAULT_MENU_AI_STATUS)
                ).getId());
    }

    @Override
    @Transactional
    public void upsertMealMenu(Long mealScheduleId, Long menuId, String cornerName, int displayOrder) {
        MealMenu mealMenu = mealMenuJpaRepository.findByMealScheduleIdAndDisplayOrder(mealScheduleId, displayOrder)
                .orElse(null);

        if (mealMenu == null) {
            mealMenuJpaRepository.save(MealMenu.create(
                    mealScheduleId,
                    menuId,
                    cornerName,
                    displayOrder,
                    INGREDIENT_SOURCE_TYPE_CRAWL,
                    INGREDIENT_STATUS_PENDING
            ));
            return;
        }

        mealMenu.updateMenu(menuId, cornerName, INGREDIENT_SOURCE_TYPE_CRAWL, INGREDIENT_STATUS_PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findAnalyzedMenuIds(Set<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return Set.of();
        }
        return menuAiAnalysisJpaRepository.findAnalyzedMenuIds(menuIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> findMenuNamesByIds(Set<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return Map.of();
        }

        List<Menu> menus = menuJpaRepository.findByIdIn(menuIds);
        Map<Long, String> menuNames = new HashMap<>();
        for (Menu menu : menus) {
            menuNames.put(menu.getId(), menu.getName());
        }
        return menuNames;
    }

    @Override
    @Transactional
    public void saveMenuAnalysis(
            Long menuId,
            String status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            List<MenuIngredientCandidate> ingredients
    ) {
        MenuAiAnalysis analysis = menuAiAnalysisJpaRepository.save(
                MenuAiAnalysis.create(menuId, status, modelName, modelVersion, reason, analyzedAt)
        );

        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }

        List<MenuAiAnalysisIngredient> entities = ingredients.stream()
                .filter(ingredient -> ingredient.ingredientCode() != null && !ingredient.ingredientCode().isBlank())
                .map(ingredient -> MenuAiAnalysisIngredient.create(
                        analysis.getId(),
                        ingredient.ingredientCode().trim(),
                        ingredient.confidence()
                ))
                .toList();

        menuAiAnalysisIngredientJpaRepository.saveAll(entities);
    }

    @Override
    @Transactional
    public void updateMenuAiStatus(Long menuId, String aiStatus, LocalDateTime analyzedAt) {
        Menu menu = menuJpaRepository.findById(menuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        menu.updateAiAnalysis(aiStatus, analyzedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<MenuTranslationKey> findExistingMenuTranslationKeys(Set<Long> menuIds, List<String> langCodes) {
        if (menuIds.isEmpty() || langCodes == null || langCodes.isEmpty()) {
            return Set.of();
        }

        List<MenuTranslation> translations = menuTranslationJpaRepository.findByMenuIdInAndLangCodeIn(menuIds, langCodes);
        Set<MenuTranslationKey> keys = new HashSet<>();
        for (MenuTranslation translation : translations) {
            keys.add(new MenuTranslationKey(translation.getMenuId(), translation.getLangCode()));
        }
        return keys;
    }

    @Override
    @Transactional
    public void saveMenuTranslation(Long menuId, String langCode, String translatedName) {
        menuTranslationJpaRepository.save(MenuTranslation.create(menuId, langCode, translatedName));
    }
}

