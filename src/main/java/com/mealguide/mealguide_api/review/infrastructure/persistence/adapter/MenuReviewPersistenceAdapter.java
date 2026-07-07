package com.mealguide.mealguide_api.review.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.review.application.dto.MenuReviewCommentRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewTargetRow;
import com.mealguide.mealguide_api.review.application.port.MenuReviewPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;
import com.mealguide.mealguide_api.review.domain.MenuReview;
import com.mealguide.mealguide_api.review.domain.MenuReviewComment;
import com.mealguide.mealguide_api.review.infrastructure.persistence.repository.MenuReviewCommentJpaRepository;
import com.mealguide.mealguide_api.review.infrastructure.persistence.repository.MenuReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MenuReviewPersistenceAdapter implements MenuReviewPort {

    private final MenuReviewJpaRepository menuReviewJpaRepository;
    private final MenuReviewCommentJpaRepository menuReviewCommentJpaRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Optional<MenuReviewTargetRow> findTargetByMealMenuId(Long mealMenuId) {
        String sql = """
                select mm.id as meal_menu_id, ms.cafeteria_id, mm.menu_id, ms.meal_date
                from meal_menu mm
                join meal_schedule ms on ms.id = mm.meal_schedule_id
                where mm.id = :mealMenuId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("mealMenuId", mealMenuId);
        List<MenuReviewTargetRow> rows = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MenuReviewTargetRow(
                rs.getLong("meal_menu_id"),
                rs.getLong("cafeteria_id"),
                rs.getLong("menu_id"),
                rs.getObject("meal_date", LocalDate.class)
        ));
        return rows.stream().findFirst();
    }

    @Override
    @Transactional
    public void ensureAnonymousParticipant(Long cafeteriaId, Long menuId, Long userId) {
        String findSql = """
                select anonymous_no
                from menu_review_anonymous_participant
                where cafeteria_id = :cafeteriaId
                  and menu_id = :menuId
                  and user_id = :userId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId)
                .addValue("userId", userId);
        List<Long> existing = namedParameterJdbcTemplate.query(findSql, params, (rs, rowNum) -> rs.getLong("anonymous_no"));
        if (!existing.isEmpty()) {
            return;
        }

        String lockSql = """
                select pg_advisory_xact_lock(hashtextextended(:lockKey, 0))
                """;
        namedParameterJdbcTemplate.query(
                lockSql,
                new MapSqlParameterSource("lockKey", cafeteriaId + ":" + menuId),
                rs -> {
                }
        );

        existing = namedParameterJdbcTemplate.query(findSql, params, (rs, rowNum) -> rs.getLong("anonymous_no"));
        if (!existing.isEmpty()) {
            return;
        }

        String insertSql = """
                insert into menu_review_anonymous_participant (
                    cafeteria_id, menu_id, user_id, anonymous_no, first_participated_at, created_at
                )
                values (
                    :cafeteriaId,
                    :menuId,
                    :userId,
                    coalesce((
                        select max(anonymous_no)
                        from menu_review_anonymous_participant
                        where cafeteria_id = :cafeteriaId
                          and menu_id = :menuId
                    ), 0) + 1,
                    now(),
                    now()
                )
                """;
        namedParameterJdbcTemplate.update(insertSql, params);
    }

    @Override
    @Transactional
    public Long saveReview(Long userId, Long cafeteriaId, Long menuId, Long mealMenuId, LocalDate mealDate, String content) {
        return menuReviewJpaRepository.save(MenuReview.create(
                userId, cafeteriaId, menuId, mealMenuId, mealDate, content
        )).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MenuReviewRow> findActiveReviewById(Long reviewId) {
        String sql = """
                select mr.id as review_id, mr.user_id, cast(null as varchar) as writer_name,
                       (mr.user_id is null or u.id is null or u.status <> 'ACTIVE' or u.deleted_at is not null) as writer_deleted,
                       mr.cafeteria_id, mr.menu_id, mr.meal_menu_id, mr.meal_date,
                       mr.content, mr.like_count, mr.comment_count, mr.created_at, mr.updated_at
                from menu_review mr
                left join users u on u.id = mr.user_id
                where mr.id = :reviewId
                  and mr.deleted_at is null
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("reviewId", reviewId);
        List<MenuReviewRow> rows = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MenuReviewRow(
                rs.getLong("review_id"),
                rs.getObject("user_id", Long.class),
                rs.getString("writer_name"),
                rs.getBoolean("writer_deleted"),
                rs.getLong("cafeteria_id"),
                rs.getLong("menu_id"),
                rs.getObject("meal_menu_id", Long.class),
                rs.getObject("meal_date", LocalDate.class),
                rs.getString("content"),
                rs.getLong("like_count"),
                rs.getLong("comment_count"),
                false,
                null,
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ));
        return rows.stream().findFirst();
    }

    @Override
    @Transactional
    public void updateReviewContent(Long reviewId, String content) {
        String sql = """
                update menu_review
                set content = :content,
                    updated_at = now()
                where id = :reviewId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("reviewId", reviewId)
                .addValue("content", content));
    }

    @Override
    @Transactional
    public void softDeleteReview(Long reviewId) {
        String sql = """
                update menu_review
                set deleted_at = now(),
                    updated_at = now()
                where id = :reviewId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("reviewId", reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveReviews(Long cafeteriaId, Long menuId) {
        String sql = """
                select count(*)
                from menu_review
                where cafeteria_id = :cafeteriaId
                  and menu_id = :menuId
                  and deleted_at is null
                """;
        Long count = namedParameterJdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<MenuLikeTarget, Long> countActiveReviewsByTargets(Set<MenuLikeTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return Map.of();
        }
        String sql = """
                with targets(cafeteria_id, menu_id) as (
                    %s
                )
                select t.cafeteria_id, t.menu_id, count(mr.id) as review_count
                from targets t
                left join menu_review mr
                  on mr.cafeteria_id = t.cafeteria_id
                 and mr.menu_id = t.menu_id
                 and mr.deleted_at is null
                group by t.cafeteria_id, t.menu_id
                """;
        sql = sql.formatted(buildTargetValuesSql(targets));
        Map<MenuLikeTarget, Long> map = new HashMap<>();
        namedParameterJdbcTemplate.query(sql, buildTargetPairParams(targets), rs -> {
            MenuLikeTarget key = new MenuLikeTarget(rs.getLong("cafeteria_id"), rs.getLong("menu_id"));
            map.put(key, rs.getLong("review_count"));
        });
        return map;
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

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> findAnonymousNamesByMenuTargetAndUserIds(Long cafeteriaId, Long menuId, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                select user_id, anonymous_no as anon_no
                from menu_review_anonymous_participant
                where cafeteria_id = :cafeteriaId
                  and menu_id = :menuId
                  and user_id is not null
                  and user_id in (:userIds)
                """;
        Map<Long, String> anonymousNames = new HashMap<>();
        namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId)
                .addValue("userIds", userIds), rs -> {
            long userId = rs.getLong("user_id");
            long anonNo = rs.getLong("anon_no");
            anonymousNames.put(userId, "Anonymous " + anonNo);
        });
        return anonymousNames;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuReviewRow> findReviewPage(Long userId, Long cafeteriaId, Long menuId, int page, int size) {
        String sql = """
                select mr.id as review_id, mr.user_id, cast(null as varchar) as writer_name,
                       (mr.user_id is null or u.id is null or u.status <> 'ACTIVE' or u.deleted_at is not null) as writer_deleted,
                       mr.cafeteria_id, mr.menu_id, mr.meal_menu_id, mr.meal_date,
                       mr.content, mr.like_count, mr.comment_count,
                       (mrl.id is not null) as liked_by_me,
                       anon.anonymous_no,
                       mr.created_at, mr.updated_at
                from menu_review mr
                left join users u on u.id = mr.user_id
                left join menu_review_like mrl
                    on mrl.review_id = mr.id
                    and mrl.user_id = :userId
                left join menu_review_anonymous_participant anon
                    on anon.cafeteria_id = mr.cafeteria_id
                    and anon.menu_id = mr.menu_id
                    and anon.user_id = mr.user_id
                where mr.cafeteria_id = :cafeteriaId
                  and mr.menu_id = :menuId
                  and mr.deleted_at is null
                order by mr.meal_date desc nulls last, mr.like_count desc, mr.created_at desc, mr.id desc
                limit :size offset :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId)
                .addValue("size", size)
                .addValue("offset", (long) page * size);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MenuReviewRow(
                rs.getLong("review_id"),
                rs.getObject("user_id", Long.class),
                rs.getString("writer_name"),
                rs.getBoolean("writer_deleted"),
                rs.getLong("cafeteria_id"),
                rs.getLong("menu_id"),
                rs.getObject("meal_menu_id", Long.class),
                rs.getObject("meal_date", LocalDate.class),
                rs.getString("content"),
                rs.getLong("like_count"),
                rs.getLong("comment_count"),
                rs.getBoolean("liked_by_me"),
                rs.getObject("anonymous_no", Long.class),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsReviewLike(Long reviewId, Long userId) {
        String sql = """
                select exists(
                    select 1 from menu_review_like
                    where review_id = :reviewId and user_id = :userId
                )
                """;
        Boolean exists = namedParameterJdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
                .addValue("reviewId", reviewId)
                .addValue("userId", userId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    @Transactional
    public void saveReviewLike(Long reviewId, Long userId) {
        String sql = """
                insert into menu_review_like (review_id, user_id, created_at)
                values (:reviewId, :userId, now())
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("reviewId", reviewId)
                .addValue("userId", userId));
    }

    @Override
    @Transactional
    public void deleteReviewLike(Long reviewId, Long userId) {
        String sql = """
                delete from menu_review_like
                where review_id = :reviewId and user_id = :userId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("reviewId", reviewId)
                .addValue("userId", userId));
    }

    @Override
    @Transactional
    public void incrementReviewLikeCount(Long reviewId) {
        String sql = """
                update menu_review
                set like_count = like_count + 1,
                    updated_at = now()
                where id = :reviewId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("reviewId", reviewId));
    }

    @Override
    @Transactional
    public void decrementReviewLikeCount(Long reviewId) {
        String sql = """
                update menu_review
                set like_count = greatest(like_count - 1, 0),
                    updated_at = now()
                where id = :reviewId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("reviewId", reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findReviewLikeCount(Long reviewId) {
        String sql = """
                select like_count
                from menu_review
                where id = :reviewId
                  and deleted_at is null
                """;
        List<Long> counts = namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource("reviewId", reviewId),
                (rs, rowNum) -> rs.getLong("like_count"));
        return counts.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuReviewCommentRow> findCommentPage(Long reviewId, Long cafeteriaId, Long menuId, int page, int size) {
        String sql = """
                select c.id as comment_id, c.review_id, c.user_id, cast(null as varchar) as writer_name,
                       (c.user_id is null or u.id is null or u.status <> 'ACTIVE' or u.deleted_at is not null) as writer_deleted,
                       anon.anonymous_no,
                       c.content, c.created_at, c.updated_at
                from menu_review_comment c
                left join users u on u.id = c.user_id
                left join menu_review_anonymous_participant anon
                    on anon.cafeteria_id = :cafeteriaId
                    and anon.menu_id = :menuId
                    and anon.user_id = c.user_id
                where c.review_id = :reviewId
                  and c.deleted_at is null
                order by c.created_at asc, c.id asc
                limit :size offset :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("reviewId", reviewId)
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId)
                .addValue("size", size)
                .addValue("offset", (long) page * size);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MenuReviewCommentRow(
                rs.getLong("comment_id"),
                rs.getLong("review_id"),
                rs.getObject("user_id", Long.class),
                rs.getString("writer_name"),
                rs.getBoolean("writer_deleted"),
                rs.getObject("anonymous_no", Long.class),
                rs.getString("content"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveComments(Long reviewId) {
        String sql = """
                select count(*)
                from menu_review_comment
                where review_id = :reviewId
                  and deleted_at is null
                """;
        Long count = namedParameterJdbcTemplate.queryForObject(sql, new MapSqlParameterSource("reviewId", reviewId), Long.class);
        return count == null ? 0L : count;
    }

    @Override
    @Transactional
    public Long saveComment(Long reviewId, Long userId, String content) {
        return menuReviewCommentJpaRepository.save(MenuReviewComment.create(reviewId, userId, content)).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MenuReviewCommentRow> findActiveCommentById(Long commentId) {
        String sql = """
                select c.id as comment_id, c.review_id, c.user_id, cast(null as varchar) as writer_name,
                       (c.user_id is null or u.id is null or u.status <> 'ACTIVE' or u.deleted_at is not null) as writer_deleted,
                       c.content, c.created_at, c.updated_at
                from menu_review_comment c
                left join users u on u.id = c.user_id
                where c.id = :commentId
                  and c.deleted_at is null
                """;
        List<MenuReviewCommentRow> rows = namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource("commentId", commentId),
                (rs, rowNum) -> new MenuReviewCommentRow(
                        rs.getLong("comment_id"),
                        rs.getLong("review_id"),
                        rs.getObject("user_id", Long.class),
                        rs.getString("writer_name"),
                        rs.getBoolean("writer_deleted"),
                        null,
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                ));
        return rows.stream().findFirst();
    }

    @Override
    @Transactional
    public void updateCommentContent(Long commentId, String content) {
        String sql = """
                update menu_review_comment
                set content = :content,
                    updated_at = now()
                where id = :commentId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("commentId", commentId)
                .addValue("content", content));
    }

    @Override
    @Transactional
    public void softDeleteComment(Long commentId) {
        String sql = """
                update menu_review_comment
                set deleted_at = now(),
                    updated_at = now()
                where id = :commentId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("commentId", commentId));
    }

    @Override
    @Transactional
    public void incrementReviewCommentCount(Long reviewId) {
        String sql = """
                update menu_review
                set comment_count = comment_count + 1,
                    updated_at = now()
                where id = :reviewId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("reviewId", reviewId));
    }

    @Override
    @Transactional
    public void decrementReviewCommentCount(Long reviewId) {
        String sql = """
                update menu_review
                set comment_count = greatest(comment_count - 1, 0),
                    updated_at = now()
                where id = :reviewId
                """;
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("reviewId", reviewId));
    }
}
