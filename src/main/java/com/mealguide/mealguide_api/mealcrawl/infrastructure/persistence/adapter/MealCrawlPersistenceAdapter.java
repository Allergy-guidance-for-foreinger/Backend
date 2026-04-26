package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

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
            try {
                mealMenuJpaRepository.save(MealMenu.create(
                        mealScheduleId,
                        menuId,
                        cornerName,
                        displayOrder,
                        INGREDIENT_SOURCE_TYPE_CRAWL,
                        INGREDIENT_STATUS_PENDING
                ));
            } catch (DataIntegrityViolationException exception) {
                MealMenu existingMealMenu = mealMenuJpaRepository.findByMealScheduleIdAndDisplayOrder(mealScheduleId, displayOrder)
                        .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR, exception));
                existingMealMenu.updateMenu(menuId, cornerName, INGREDIENT_SOURCE_TYPE_CRAWL, INGREDIENT_STATUS_PENDING);
            }
            return;
        }

        mealMenu.updateMenu(menuId, cornerName, INGREDIENT_SOURCE_TYPE_CRAWL, INGREDIENT_STATUS_PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyMealCacheRow> findWeeklyMealsForCache(Long cafeteriaId, LocalDate weekStartDate, LocalDate weekEndDate) {
        return mealMenuJpaRepository.findWeeklyMealsForCache(cafeteriaId, weekStartDate, weekEndDate);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsCafeteriaInSchool(Long cafeteriaId, Long schoolId) {
        return cafeteriaJpaRepository.existsByIdAndSchoolId(cafeteriaId, schoolId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> findTranslatedMenuNamesByMealMenuIds(Set<Long> mealMenuIds, String langCode) {
        if (mealMenuIds == null || mealMenuIds.isEmpty() || langCode == null || langCode.isBlank()) {
            return Map.of();
        }

        String sql = """
                select mm.id as meal_menu_id,
                       mt.name as translated_name
                from meal_menu mm
                join menu_translation mt on mt.menu_id = mm.menu_id
                where mm.id in (:mealMenuIds)
                  and mt.lang_code = :langCode
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mealMenuIds", mealMenuIds)
                .addValue("langCode", langCode);

        Map<Long, String> translatedMenuNames = new HashMap<>();
        namedParameterJdbcTemplate.query(sql, params, rs -> {
            translatedMenuNames.put(
                    rs.getLong("meal_menu_id"),
                    rs.getString("translated_name")
            );
        });
        return translatedMenuNames;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealMenuIngredientRow> findConfirmedIngredientsByMealMenuIds(Set<Long> mealMenuIds) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                select mmci.meal_menu_id,
                       mmci.ingredient_code,
                       i.name as ingredient_name
                from meal_menu_confirmed_ingredient mmci
                join ingredient i on i.code = mmci.ingredient_code
                where mmci.meal_menu_id in (:mealMenuIds)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("mealMenuIds", mealMenuIds);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MealMenuIngredientRow(
                rs.getLong("meal_menu_id"),
                rs.getString("ingredient_code"),
                rs.getString("ingredient_name")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findMealMenuIdsHavingConfirmedIngredients(Set<Long> mealMenuIds) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return Set.of();
        }

        String sql = """
                select distinct mmci.meal_menu_id
                from meal_menu_confirmed_ingredient mmci
                where mmci.meal_menu_id in (:mealMenuIds)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("mealMenuIds", mealMenuIds);
        return new HashSet<>(namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> rs.getLong("meal_menu_id")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealMenuIngredientRow> findAiIngredientsByMealMenuIds(Set<Long> mealMenuIds) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                with target_meal_menu as (
                    select mm.id as meal_menu_id, mm.menu_id
                    from meal_menu mm
                    where mm.id in (:mealMenuIds)
                ),
                latest_success_analysis as (
                    select maa.menu_id, max(coalesce(maa.analyzed_at, maa.created_at)) as latest_at
                    from menu_ai_analysis maa
                    join target_meal_menu tmm on tmm.menu_id = maa.menu_id
                    where maa.status = 'SUCCESS'
                    group by maa.menu_id
                ),
                latest_analysis_id as (
                    select maa.id, maa.menu_id
                    from menu_ai_analysis maa
                    join latest_success_analysis lsa
                      on lsa.menu_id = maa.menu_id
                     and coalesce(maa.analyzed_at, maa.created_at) = lsa.latest_at
                    where maa.status = 'SUCCESS'
                )
                select tmm.meal_menu_id,
                       mai.ingredient_code,
                       i.name as ingredient_name
                from target_meal_menu tmm
                join latest_analysis_id lai on lai.menu_id = tmm.menu_id
                join menu_ai_analysis_ingredient mai on mai.menu_ai_analysis_id = lai.id
                join ingredient i on i.code = mai.ingredient_code
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("mealMenuIds", mealMenuIds);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MealMenuIngredientRow(
                rs.getLong("meal_menu_id"),
                rs.getString("ingredient_code"),
                rs.getString("ingredient_name")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findMealMenuIdsHavingAiIngredients(Set<Long> mealMenuIds) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return Set.of();
        }

        String sql = """
                with target_meal_menu as (
                    select mm.id as meal_menu_id, mm.menu_id
                    from meal_menu mm
                    where mm.id in (:mealMenuIds)
                ),
                latest_success_analysis as (
                    select maa.menu_id, max(coalesce(maa.analyzed_at, maa.created_at)) as latest_at
                    from menu_ai_analysis maa
                    join target_meal_menu tmm on tmm.menu_id = maa.menu_id
                    where maa.status = 'SUCCESS'
                    group by maa.menu_id
                ),
                latest_analysis_id as (
                    select maa.id, maa.menu_id
                    from menu_ai_analysis maa
                    join latest_success_analysis lsa
                      on lsa.menu_id = maa.menu_id
                     and coalesce(maa.analyzed_at, maa.created_at) = lsa.latest_at
                    where maa.status = 'SUCCESS'
                )
                select distinct tmm.meal_menu_id
                from target_meal_menu tmm
                join latest_analysis_id lai on lai.menu_id = tmm.menu_id
                join menu_ai_analysis_ingredient mai on mai.menu_ai_analysis_id = lai.id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("mealMenuIds", mealMenuIds);
        return new HashSet<>(namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> rs.getLong("meal_menu_id")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestrictionIngredientRow> findAllergyRestrictionIngredients(Set<String> allergyCodes) {
        if (allergyCodes == null || allergyCodes.isEmpty()) {
            return List.of();
        }

        String sql = """
                select ai.allergy_code as restriction_code,
                       ai.ingredient_code,
                       i.name as ingredient_name
                from allergy_ingredient ai
                join ingredient i on i.code = ai.ingredient_code
                where ai.allergy_code in (:allergyCodes)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("allergyCodes", allergyCodes);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new RestrictionIngredientRow(
                rs.getString("restriction_code"),
                rs.getString("ingredient_code"),
                rs.getString("ingredient_name")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestrictionIngredientRow> findReligiousRestrictionIngredients(String religiousCode) {
        if (religiousCode == null || religiousCode.isBlank()) {
            return List.of();
        }

        String sql = """
                select rfri.religious_food_restriction_code as restriction_code,
                       rfri.ingredient_code,
                       i.name as ingredient_name
                from religious_food_restriction_ingredient rfri
                join ingredient i on i.code = rfri.ingredient_code
                where rfri.religious_food_restriction_code = :religiousCode
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("religiousCode", religiousCode);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new RestrictionIngredientRow(
                rs.getString("restriction_code"),
                rs.getString("ingredient_code"),
                rs.getString("ingredient_name")
        ));
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

