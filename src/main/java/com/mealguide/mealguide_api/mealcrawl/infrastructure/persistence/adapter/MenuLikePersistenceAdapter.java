package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.mealcrawl.application.port.MenuLikePort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLike;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository.MenuLikeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MenuLikePersistenceAdapter implements MenuLikePort {

    private final MenuLikeJpaRepository menuLikeJpaRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Optional<MenuLikeTarget> findTargetByMealMenuId(Long mealMenuId) {
        String sql = """
                select ms.cafeteria_id, mm.menu_id
                from meal_menu mm
                join meal_schedule ms on ms.id = mm.meal_schedule_id
                where mm.id = :mealMenuId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("mealMenuId", mealMenuId);
        List<MenuLikeTarget> rows = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) ->
                new MenuLikeTarget(
                        rs.getLong("cafeteria_id"),
                        rs.getLong("menu_id")
                )
        );
        return rows.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsLike(Long userId, Long cafeteriaId, Long menuId) {
        String sql = """
                select exists(
                    select 1
                    from menu_like
                    where user_id = :userId
                      and cafeteria_id = :cafeteriaId
                      and menu_id = :menuId
                )
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId);
        Boolean exists = namedParameterJdbcTemplate.queryForObject(sql, params, Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    @Transactional
    public void saveLike(Long userId, Long cafeteriaId, Long menuId) {
        menuLikeJpaRepository.save(MenuLike.create(userId, cafeteriaId, menuId));
    }

    @Override
    @Transactional
    public void deleteLike(Long userId, Long cafeteriaId, Long menuId) {
        String sql = """
                delete from menu_like
                where user_id = :userId
                  and cafeteria_id = :cafeteriaId
                  and menu_id = :menuId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId);
        namedParameterJdbcTemplate.update(sql, params);
    }

    @Override
    @Transactional(readOnly = true)
    public long countLikes(Long cafeteriaId, Long menuId) {
        String sql = """
                select count(*)
                from menu_like
                where cafeteria_id = :cafeteriaId
                  and menu_id = :menuId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId);
        Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<MenuLikeTarget, Long> countLikesByTargets(Set<MenuLikeTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return Map.of();
        }

        String sql = """
                select ml.cafeteria_id, ml.menu_id, count(*) as like_count
                from menu_like ml
                where ml.cafeteria_id in (:cafeteriaIds)
                  and ml.menu_id in (:menuIds)
                group by ml.cafeteria_id, ml.menu_id
                """;

        MapSqlParameterSource params = buildTargetParams(targets);
        Map<MenuLikeTarget, Long> result = new HashMap<>();
        namedParameterJdbcTemplate.query(sql, params, rs -> {
            MenuLikeTarget key = new MenuLikeTarget(rs.getLong("cafeteria_id"), rs.getLong("menu_id"));
            if (targets.contains(key)) {
                result.put(key, rs.getLong("like_count"));
            }
        });
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<MenuLikeTarget> findLikedTargetsByUser(Long userId, Set<MenuLikeTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return Set.of();
        }

        String sql = """
                select ml.cafeteria_id, ml.menu_id
                from menu_like ml
                where ml.user_id = :userId
                  and ml.cafeteria_id in (:cafeteriaIds)
                  and ml.menu_id in (:menuIds)
                """;

        MapSqlParameterSource params = buildTargetParams(targets).addValue("userId", userId);
        Set<MenuLikeTarget> result = new HashSet<>();
        namedParameterJdbcTemplate.query(sql, params, rs -> {
            MenuLikeTarget key = new MenuLikeTarget(rs.getLong("cafeteria_id"), rs.getLong("menu_id"));
            if (targets.contains(key)) {
                result.add(key);
            }
        });
        return result;
    }

    private MapSqlParameterSource buildTargetParams(Set<MenuLikeTarget> targets) {
        List<Long> cafeteriaIds = targets.stream().map(MenuLikeTarget::cafeteriaId).distinct().toList();
        List<Long> menuIds = targets.stream().map(MenuLikeTarget::menuId).distinct().toList();
        return new MapSqlParameterSource()
                .addValue("cafeteriaIds", cafeteriaIds)
                .addValue("menuIds", menuIds);
    }
}
