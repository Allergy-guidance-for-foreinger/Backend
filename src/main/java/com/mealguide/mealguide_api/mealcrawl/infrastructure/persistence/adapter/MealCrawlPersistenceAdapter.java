package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealMenuMatchedAllergyRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MenuDetailRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.NamedIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.RestrictionIngredientRow;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import com.mealguide.mealguide_api.mealcrawl.domain.MealMenu;
import com.mealguide.mealguide_api.mealcrawl.domain.MealSchedule;
import com.mealguide.mealguide_api.mealcrawl.domain.MealScheduleCrawlHistory;
import com.mealguide.mealguide_api.mealcrawl.domain.Menu;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysis;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisAllergy;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisIngredient;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAllergyCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuIngredientCandidate;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuSpicyLevel;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslation;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslationKey;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.CafeteriaJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MealMenuJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MealScheduleCrawlHistoryJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MealScheduleJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuAiAnalysisIngredientJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuAiAnalysisJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuAiAnalysisAllergyJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuJpaRepository;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuTranslationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealCrawlPersistenceAdapter implements MealCrawlPersistencePort {

    private static final MenuAiStatus DEFAULT_MENU_AI_STATUS = MenuAiStatus.FAILED;
    private static final String INGREDIENT_SOURCE_TYPE_CRAWL = "CRAWL";

    private final CafeteriaJpaRepository cafeteriaJpaRepository;
    private final MealScheduleCrawlHistoryJpaRepository crawlHistoryJpaRepository;
    private final MealScheduleJpaRepository mealScheduleJpaRepository;
    private final MenuJpaRepository menuJpaRepository;
    private final MealMenuJpaRepository mealMenuJpaRepository;
    private final MenuAiAnalysisJpaRepository menuAiAnalysisJpaRepository;
    private final MenuAiAnalysisIngredientJpaRepository menuAiAnalysisIngredientJpaRepository;
    private final MenuAiAnalysisAllergyJpaRepository menuAiAnalysisAllergyJpaRepository;
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
                        INGREDIENT_SOURCE_TYPE_CRAWL
                ));
            } catch (DataIntegrityViolationException exception) {
                MealMenu existingMealMenu = mealMenuJpaRepository.findByMealScheduleIdAndDisplayOrder(mealScheduleId, displayOrder)
                        .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR, exception));
                existingMealMenu.updateMenu(menuId, cornerName, INGREDIENT_SOURCE_TYPE_CRAWL);
            }
            return;
        }

        mealMenu.updateMenu(menuId, cornerName, INGREDIENT_SOURCE_TYPE_CRAWL);
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
                latest_analysis_id as (
                    select ranked.id, ranked.menu_id
                    from (
                        select maa.id,
                               maa.menu_id,
                               row_number() over (
                                   partition by maa.menu_id
                                   order by coalesce(maa.analyzed_at, maa.created_at) desc, maa.id desc
                               ) as rn
                        from menu_ai_analysis maa
                        join target_meal_menu tmm on tmm.menu_id = maa.menu_id
                        where maa.status = 'SUCCESS'
                    ) ranked
                    where ranked.rn = 1
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
                latest_analysis_id as (
                    select ranked.id, ranked.menu_id
                    from (
                        select maa.id,
                               maa.menu_id,
                               row_number() over (
                                   partition by maa.menu_id
                                   order by coalesce(maa.analyzed_at, maa.created_at) desc, maa.id desc
                               ) as rn
                        from menu_ai_analysis maa
                        join target_meal_menu tmm on tmm.menu_id = maa.menu_id
                        where maa.status = 'SUCCESS'
                    ) ranked
                    where ranked.rn = 1
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
    public Optional<MenuDetailRow> findMenuDetailByMealMenuId(Long mealMenuId) {
        String sql = """
                select mm.id as meal_menu_id,
                       ms.cafeteria_id,
                       mm.menu_id,
                       m.name as menu_name,
                       mm.corner_name,
                       mm.display_order,
                       m.spicy_level,
                       m.ai_analysis_status,
                       c.school_id
                from meal_menu mm
                join menu m on m.id = mm.menu_id
                join meal_schedule ms on ms.id = mm.meal_schedule_id
                join cafeteria c on c.id = ms.cafeteria_id
                where mm.id = :mealMenuId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("mealMenuId", mealMenuId);
        List<MenuDetailRow> rows = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MenuDetailRow(
                rs.getLong("meal_menu_id"),
                rs.getLong("cafeteria_id"),
                rs.getLong("menu_id"),
                rs.getString("menu_name"),
                rs.getString("corner_name"),
                rs.getInt("display_order"),
                rs.getLong("spicy_level"),
                rs.getString("ai_analysis_status"),
                rs.getLong("school_id")
        ));
        return rows.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuDetailRow> findMenuDetailsByMealMenuIds(Set<Long> mealMenuIds) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                select mm.id as meal_menu_id,
                       ms.cafeteria_id,
                       mm.menu_id,
                       m.name as menu_name,
                       mm.corner_name,
                       mm.display_order,
                       m.spicy_level,
                       m.ai_analysis_status,
                       c.school_id
                from meal_menu mm
                join menu m on m.id = mm.menu_id
                join meal_schedule ms on ms.id = mm.meal_schedule_id
                join cafeteria c on c.id = ms.cafeteria_id
                where mm.id in (:mealMenuIds)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("mealMenuIds", mealMenuIds);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MenuDetailRow(
                rs.getLong("meal_menu_id"),
                rs.getLong("cafeteria_id"),
                rs.getLong("menu_id"),
                rs.getString("menu_name"),
                rs.getString("corner_name"),
                rs.getInt("display_order"),
                rs.getLong("spicy_level"),
                rs.getString("ai_analysis_status"),
                rs.getLong("school_id")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findTranslatedMenuNameByMealMenuId(Long mealMenuId, String langCode) {
        if (langCode == null || langCode.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                select mt.name as translated_name
                from meal_menu mm
                join menu_translation mt on mt.menu_id = mm.menu_id
                where mm.id = :mealMenuId
                  and mt.lang_code = :langCode
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mealMenuId", mealMenuId)
                .addValue("langCode", langCode.trim().toLowerCase());
        List<String> names = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("translated_name"));
        return names.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NamedIngredientRow> findConfirmedIngredientsForMenuDetail(Long mealMenuId, String langCode) {
        String sql = """
                select mmci.ingredient_code as code,
                       coalesce(it.name, i.name) as name
                from meal_menu_confirmed_ingredient mmci
                join ingredient i on i.code = mmci.ingredient_code
                left join ingredient_translation it
                  on it.ingredient_code = i.code
                 and it.lang_code = :langCode
                where mmci.meal_menu_id = :mealMenuId
                order by mmci.ingredient_code
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mealMenuId", mealMenuId)
                .addValue("langCode", normalizeLanguageCode(langCode));
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new NamedIngredientRow(
                rs.getString("code"),
                rs.getString("name")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NamedIngredientRow> findAiIngredientsForMenuDetail(Long mealMenuId, String langCode) {
        String sql = """
                with target_menu as (
                    select mm.menu_id
                    from meal_menu mm
                    where mm.id = :mealMenuId
                ),
                latest_success_analysis as (
                    select maa.id
                    from menu_ai_analysis maa
                    join target_menu tm on tm.menu_id = maa.menu_id
                    where maa.status = 'SUCCESS'
                    order by coalesce(maa.analyzed_at, maa.created_at) desc, maa.id desc
                    limit 1
                )
                select mai.ingredient_code as code,
                       coalesce(it.name, i.name) as name
                from latest_success_analysis lsa
                join menu_ai_analysis_ingredient mai on mai.menu_ai_analysis_id = lsa.id
                join ingredient i on i.code = mai.ingredient_code
                left join ingredient_translation it
                  on it.ingredient_code = i.code
                 and it.lang_code = :langCode
                order by mai.ingredient_code
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mealMenuId", mealMenuId)
                .addValue("langCode", normalizeLanguageCode(langCode));
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new NamedIngredientRow(
                rs.getString("code"),
                rs.getString("name")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealMenuIngredientRow> findConfirmedIngredientsForMenuDetails(Set<Long> mealMenuIds, String langCode) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                select mmci.meal_menu_id,
                       mmci.ingredient_code,
                       coalesce(it.name, i.name) as ingredient_name
                from meal_menu_confirmed_ingredient mmci
                join ingredient i on i.code = mmci.ingredient_code
                left join ingredient_translation it
                  on it.ingredient_code = i.code
                 and it.lang_code = :langCode
                where mmci.meal_menu_id in (:mealMenuIds)
                order by mmci.meal_menu_id, mmci.ingredient_code
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mealMenuIds", mealMenuIds)
                .addValue("langCode", normalizeLanguageCode(langCode));
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MealMenuIngredientRow(
                rs.getLong("meal_menu_id"),
                rs.getString("ingredient_code"),
                rs.getString("ingredient_name")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealMenuIngredientRow> findAiIngredientsForMenuDetails(Set<Long> mealMenuIds, String langCode) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                with target_meal_menu as (
                    select mm.id as meal_menu_id, mm.menu_id
                    from meal_menu mm
                    where mm.id in (:mealMenuIds)
                ),
                latest_analysis_id as (
                    select id, menu_id
                    from (
                            select maa.id, maa.menu_id,
                                row_number() over (partition by maa.menu_id order by coalesce(maa.analyzed_at, maa.created_at) desc, maa.id desc) as rn
                         from menu_ai_analysis maa
                         join target_meal_menu tmm on tmm.menu_id = maa.menu_id
                         where maa.status = 'SUCCESS'
                         ) t
                          where rn = 1
                )
                select tmm.meal_menu_id,
                       mai.ingredient_code,
                       coalesce(it.name, i.name) as ingredient_name
                from target_meal_menu tmm
                join latest_analysis_id lai on lai.menu_id = tmm.menu_id
                join menu_ai_analysis_ingredient mai on mai.menu_ai_analysis_id = lai.id
                join ingredient i on i.code = mai.ingredient_code
                left join ingredient_translation it
                  on it.ingredient_code = i.code
                 and it.lang_code = :langCode
                order by tmm.meal_menu_id, mai.ingredient_code
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mealMenuIds", mealMenuIds)
                .addValue("langCode", normalizeLanguageCode(langCode));
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MealMenuIngredientRow(
                rs.getLong("meal_menu_id"),
                rs.getString("ingredient_code"),
                rs.getString("ingredient_name")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MealMenuMatchedAllergyRow> findMatchedAllergiesByMealMenuIds(Long userId, Set<Long> mealMenuIds, String langCode) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                with target_meal_menu as (
                    select mm.id as meal_menu_id, mm.menu_id
                    from meal_menu mm
                    where mm.id in (:mealMenuIds)
                ),
                latest_analysis_id as (
                    select id, menu_id
                    from (
                        select maa.id, maa.menu_id,
                               row_number() over (partition by maa.menu_id order by coalesce(maa.analyzed_at, maa.created_at) desc, maa.id desc) as rn
                        from menu_ai_analysis maa
                        join target_meal_menu tmm on tmm.menu_id = maa.menu_id
                        where maa.status = 'SUCCESS'
                    ) ranked
                    where ranked.rn = 1
                )
                select tmm.meal_menu_id,
                       maaallergy.allergy_code,
                       coalesce(at.name, a.name) as allergy_name,
                       maaallergy.reason
                from target_meal_menu tmm
                join latest_analysis_id lai on lai.menu_id = tmm.menu_id
                join menu_ai_analysis_allergy maaallergy on maaallergy.menu_ai_analysis_id = lai.id
                join user_allergy ua
                  on ua.user_id = :userId
                 and ua.allergy_code = maaallergy.allergy_code
                join allergy a on a.code = maaallergy.allergy_code
                left join allergy_translation at
                  on at.allergy_code = a.code
                  and at.lang_code = :langCode
                order by tmm.meal_menu_id, maaallergy.allergy_code
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("mealMenuIds", mealMenuIds)
                .addValue("langCode", normalizeLanguageCode(langCode));
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MealMenuMatchedAllergyRow(
                rs.getLong("meal_menu_id"),
                rs.getString("allergy_code"),
                rs.getString("allergy_name"),
                rs.getString("reason")
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findMealMenuIdsHavingMatchedAllergies(Long userId, Set<Long> mealMenuIds) {
        if (mealMenuIds == null || mealMenuIds.isEmpty()) {
            return Set.of();
        }

        String sql = """
                with target_meal_menu as (
                    select mm.id as meal_menu_id, mm.menu_id
                    from meal_menu mm
                    where mm.id in (:mealMenuIds)
                ),
                latest_analysis_id as (
                    select id, menu_id
                    from (
                        select maa.id, maa.menu_id,
                               row_number() over (partition by maa.menu_id order by coalesce(maa.analyzed_at, maa.created_at) desc, maa.id desc) as rn
                        from menu_ai_analysis maa
                        join target_meal_menu tmm on tmm.menu_id = maa.menu_id
                        where maa.status = 'SUCCESS'
                    ) ranked
                    where ranked.rn = 1
                )
                select distinct tmm.meal_menu_id
                from target_meal_menu tmm
                join latest_analysis_id lai on lai.menu_id = tmm.menu_id
                join menu_ai_analysis_allergy maaallergy on maaallergy.menu_ai_analysis_id = lai.id
                join user_allergy ua
                  on ua.user_id = :userId
                 and ua.allergy_code = maaallergy.allergy_code
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("mealMenuIds", mealMenuIds);
        return new HashSet<>(namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getLong("meal_menu_id")));
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
    public List<Long> findRetryTargetMenuIds(int limit, int maxAttemptCount) {
        if (limit <= 0) {
            return List.of();
        }
        return menuAiAnalysisJpaRepository.findLatestFailedMenuIdsWithAttemptBelow(
                MenuAiStatus.FAILED.name(),
                maxAttemptCount,
                limit
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> findLatestAttemptCounts(Set<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                select ranked.menu_id, ranked.attempt_count
                from (
                    select maa.menu_id,
                           maa.attempt_count,
                           row_number() over (
                               partition by maa.menu_id
                               order by coalesce(maa.analyzed_at, maa.created_at) desc, maa.id desc
                           ) as rn
                    from menu_ai_analysis maa
                    where maa.menu_id in (:menuIds)
                ) ranked
                where ranked.rn = 1
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("menuIds", menuIds);
        Map<Long, Integer> latestAttemptCounts = new HashMap<>();
        namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> Map.entry(
                rs.getLong("menu_id"),
                rs.getInt("attempt_count")
        )).forEach(entry -> latestAttemptCounts.put(entry.getKey(), entry.getValue()));
        return latestAttemptCounts;
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
            MenuAiStatus status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            int attemptCount,
            List<MenuIngredientCandidate> ingredients
    ) {
        MenuAiAnalysis analysis = upsertMenuAnalysis(
                menuId,
                status,
                modelName,
                modelVersion,
                reason,
                analyzedAt,
                attemptCount
        );

        menuAiAnalysisIngredientJpaRepository.deleteByMenuAiAnalysisId(analysis.getId());

        saveIngredientsForAnalysis(analysis.getId(), ingredients, null);
    }

    @Override
    @Transactional
    public void updateMenuAiStatus(Long menuId, MenuAiStatus aiStatus, LocalDateTime analyzedAt) {
        updateMenuAiStatus(menuId, aiStatus, analyzedAt, null);
    }

    @Override
    @Transactional
    public void updateMenuAiStatus(Long menuId, MenuAiStatus aiStatus, LocalDateTime analyzedAt, MenuSpicyLevel spicyLevel) {
        Menu menu = menuJpaRepository.findById(menuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        menu.updateAiAnalysis(aiStatus, analyzedAt, spicyLevel);
    }

    @Override
    @Transactional
    public void saveMenuAnalysisAndUpdateStatus(
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

    @Override
    @Transactional
    public void saveMenuAnalysisAndUpdateStatus(
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
        saveMenuAnalysisAndUpdateStatus(
                menuId,
                status,
                modelName,
                modelVersion,
                reason,
                analyzedAt,
                attemptCount,
                ingredients,
                validIngredientCodes,
                List.of(),
                Set.of(),
                spicyLevel
        );
    }

    @Override
    @Transactional
    public void saveMenuAnalysisAndUpdateStatus(
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
        MenuAiAnalysis analysis = upsertMenuAnalysis(
                menuId,
                status,
                modelName,
                modelVersion,
                reason,
                analyzedAt,
                attemptCount
        );

        menuAiAnalysisIngredientJpaRepository.deleteByMenuAiAnalysisId(analysis.getId());
        menuAiAnalysisAllergyJpaRepository.deleteByMenuAiAnalysisId(analysis.getId());

        saveIngredientsForAnalysis(analysis.getId(), ingredients, validIngredientCodes);
        saveAllergiesForAnalysis(menuId, analysis.getId(), allergies, validAllergyCodes);

        Menu menu = menuJpaRepository.findById(menuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        menu.updateAiAnalysis(status, analyzedAt, spicyLevel);
    }

    private MenuAiAnalysis upsertMenuAnalysis(
            Long menuId,
            MenuAiStatus status,
            String modelName,
            String modelVersion,
            String reason,
            LocalDateTime analyzedAt,
            int attemptCount
    ) {
        MenuAiAnalysis analysis = menuAiAnalysisJpaRepository.findTopLatestByMenuId(menuId)
                .orElseGet(() -> MenuAiAnalysis.create(
                        menuId,
                        status,
                        modelName,
                        modelVersion,
                        reason,
                        analyzedAt,
                        attemptCount
                ));
        analysis.update(status, modelName, modelVersion, reason, analyzedAt, attemptCount);
        return menuAiAnalysisJpaRepository.save(analysis);
    }

    private void saveIngredientsForAnalysis(
            Long menuAiAnalysisId,
            List<MenuIngredientCandidate> ingredients,
            Set<String> validIngredientCodes
    ) {
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }
        Set<String> prevalidatedCodes = validIngredientCodes == null ? Set.of() : validIngredientCodes;
        Set<String> candidateCodes = ingredients.stream()
                .filter(ingredient -> ingredient.ingredientCode() != null && !ingredient.ingredientCode().isBlank())
                .map(ingredient -> ingredient.ingredientCode().trim())
                .collect(Collectors.toSet());
        Set<String> validCodes = ensureIngredientCodesExist(candidateCodes, prevalidatedCodes);
        Map<String, MenuIngredientCandidate> deduplicated = new LinkedHashMap<>();
        for (MenuIngredientCandidate ingredient : ingredients) {
            if (ingredient.ingredientCode() == null || ingredient.ingredientCode().isBlank()) {
                continue;
            }
            String ingredientCode = ingredient.ingredientCode().trim();
            if (validCodes.contains(ingredientCode)) {
                deduplicated.putIfAbsent(ingredientCode, ingredient);
            }
        }
        if (deduplicated.isEmpty()) {
            return;
        }
        List<MenuAiAnalysisIngredient> entities = deduplicated.values().stream()
                .map(ingredient -> MenuAiAnalysisIngredient.create(
                        menuAiAnalysisId,
                        ingredient.ingredientCode().trim(),
                        ingredient.confidence()
                ))
                .toList();
        menuAiAnalysisIngredientJpaRepository.saveAll(entities);
    }

    private void saveAllergiesForAnalysis(
            Long menuId,
            Long menuAiAnalysisId,
            List<MenuAllergyCandidate> allergies,
            Set<String> validAllergyCodes
    ) {
        if (allergies == null || allergies.isEmpty()) {
            return;
        }
        Set<String> validCodes = validAllergyCodes == null ? Set.of() : validAllergyCodes;
        Map<String, MenuAllergyCandidate> deduplicatedByCode = new HashMap<>();
        for (MenuAllergyCandidate allergy : allergies) {
            if (allergy.allergyCode() == null || allergy.allergyCode().isBlank()) {
                continue;
            }
            String allergyCode = allergy.allergyCode().trim();
            if (!validCodes.contains(allergyCode)) {
                log.warn("Skipped unknown allergy_code during AI analysis save: menuId={}, analysisId={}, allergyCode={}",
                        menuId, menuAiAnalysisId, allergyCode);
                continue;
            }
            deduplicatedByCode.putIfAbsent(allergyCode, allergy);
        }
        if (deduplicatedByCode.isEmpty()) {
            return;
        }
        List<MenuAiAnalysisAllergy> entities = deduplicatedByCode.values().stream()
                .map(allergy -> MenuAiAnalysisAllergy.create(
                        menuAiAnalysisId,
                        allergy.allergyCode().trim(),
                        allergy.confidence(),
                        allergy.reason()
                ))
                .toList();
        menuAiAnalysisAllergyJpaRepository.saveAll(entities);
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

    @Override
    @Transactional
    public void saveMenuTranslations(Map<MenuTranslationKey, String> translationsByKey) {
        if (translationsByKey == null || translationsByKey.isEmpty()) {
            return;
        }
        List<MenuTranslation> entities = translationsByKey.entrySet().stream()
                .map(entry -> MenuTranslation.create(
                        entry.getKey().menuId(),
                        entry.getKey().langCode(),
                        entry.getValue()
                ))
                .toList();
        menuTranslationJpaRepository.saveAll(entities);
    }

    private String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "ko";
        }
        return languageCode.trim().toLowerCase();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findExistingIngredientCodes(Set<String> ingredientCodes) {
        if (ingredientCodes == null || ingredientCodes.isEmpty()) {
            return Set.of();
        }

        String sql = """
                select i.code
                from ingredient i
                where i.code in (:ingredientCodes)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("ingredientCodes", ingredientCodes);
        return new HashSet<>(namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("code")));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findExistingAllergyCodes(Set<String> allergyCodes) {
        if (allergyCodes == null || allergyCodes.isEmpty()) {
            return Set.of();
        }
        String sql = """
                select a.code
                from allergy a
                where a.code in (:allergyCodes)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("allergyCodes", allergyCodes);
        return new HashSet<>(namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("code")));
    }

    private Set<String> ensureIngredientCodesExist(Set<String> candidateCodes) {
        return ensureIngredientCodesExist(candidateCodes, Set.of());
    }

    private Set<String> ensureIngredientCodesExist(Set<String> candidateCodes, Set<String> prevalidatedCodes) {
        if (candidateCodes == null || candidateCodes.isEmpty()) {
            return Set.of();
        }
        Set<String> existingCodes = new HashSet<>();
        if (prevalidatedCodes != null && !prevalidatedCodes.isEmpty()) {
            existingCodes.addAll(prevalidatedCodes);
        }
        existingCodes.addAll(findExistingIngredientCodes(candidateCodes));

        Set<String> missingCodes = candidateCodes.stream()
                .filter(code -> !existingCodes.contains(code))
                .collect(Collectors.toSet());
        if (!missingCodes.isEmpty()) {
            String insertSql = """
                    insert into ingredient (code, name, created_at)
                    values (:code, :name, :createdAt)
                    on conflict (code) do nothing
                    """;
            for (String missingCode : missingCodes) {
                MapSqlParameterSource params = new MapSqlParameterSource()
                        .addValue("code", missingCode)
                        .addValue("name", missingCode)
                        .addValue("createdAt", LocalDateTime.now());
                namedParameterJdbcTemplate.update(insertSql, params);
                log.debug("Inserted missing ingredient_code from AI analysis: ingredientCode={}", missingCode);
            }
            existingCodes.addAll(findExistingIngredientCodes(candidateCodes));
        }
        return existingCodes;
    }
}

