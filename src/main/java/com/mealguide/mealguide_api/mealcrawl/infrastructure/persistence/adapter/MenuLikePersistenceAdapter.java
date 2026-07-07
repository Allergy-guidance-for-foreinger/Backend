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
                with targets(cafeteria_id, menu_id) as (
                    %s
                )
                select t.cafeteria_id, t.menu_id, count(ml.id) as like_count
                from targets t
                left join menu_like ml
                  on ml.cafeteria_id = t.cafeteria_id
                 and ml.menu_id = t.menu_id
                group by t.cafeteria_id, t.menu_id
                """.formatted(buildTargetValuesSql(targets));

        MapSqlParameterSource params = buildTargetPairParams(targets);
        Map<MenuLikeTarget, Long> result = new HashMap<>();
        namedParameterJdbcTemplate.query(sql, params, rs -> {
            MenuLikeTarget key = new MenuLikeTarget(rs.getLong("cafeteria_id"), rs.getLong("menu_id"));
            result.put(key, rs.getLong("like_count"));
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
                with targets(cafeteria_id, menu_id) as (
                    %s
                )
                select t.cafeteria_id, t.menu_id
                from targets t
                join menu_like ml
                  on ml.cafeteria_id = t.cafeteria_id
                 and ml.menu_id = t.menu_id
                 and ml.user_id = :userId
                """.formatted(buildTargetValuesSql(targets));

        MapSqlParameterSource params = buildTargetPairParams(targets).addValue("userId", userId);
        Set<MenuLikeTarget> result = new HashSet<>();
        namedParameterJdbcTemplate.query(sql, params, (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                result.add(new MenuLikeTarget(rs.getLong("cafeteria_id"), rs.getLong("menu_id")))
        );
        return result;
    }

    private String buildTargetValuesSql(Set<MenuLikeTarget> targets) {
        StringBuilder values = new StringBuilder("values ");
        int index = 0;
        for (MenuLikeTarget ignored : targets) {
            if (index > 0) {
                values.append(", ");
            }
            values.append("(:cafeteriaId").append(index).append(", :menuId").append(index).append(")");
            index++;
        }
        return values.toString();
    }

    private MapSqlParameterSource buildTargetPairParams(Set<MenuLikeTarget> targets) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        int index = 0;
        for (MenuLikeTarget target : targets) {
            params.addValue("cafeteriaId" + index, target.cafeteriaId());
            params.addValue("menuId" + index, target.menuId());
            index++;
        }
        return params;
    }

}
