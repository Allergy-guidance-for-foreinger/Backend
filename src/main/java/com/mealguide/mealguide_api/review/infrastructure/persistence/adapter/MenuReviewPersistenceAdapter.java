package com.mealguide.mealguide_api.review.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.review.application.dto.MenuReviewCommentRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewTargetRow;
import com.mealguide.mealguide_api.review.application.port.MenuReviewPort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;
import com.mealguide.mealguide_api.review.domain.MenuReview;
import com.mealguide.mealguide_api.review.domain.MenuReviewComment;
import com.mealguide.mealguide_api.review.domain.MenuReviewLike;
import com.mealguide.mealguide_api.review.infrastructure.persistence.repository.MenuReviewCommentJpaRepository;
import com.mealguide.mealguide_api.review.infrastructure.persistence.repository.MenuReviewJpaRepository;
import com.mealguide.mealguide_api.review.infrastructure.persistence.repository.MenuReviewLikeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MenuReviewPersistenceAdapter implements MenuReviewPort {

    private final MenuReviewJpaRepository menuReviewJpaRepository;
    private final MenuReviewLikeJpaRepository menuReviewLikeJpaRepository;
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
                rs.getDate("meal_date").toLocalDate()
        ));
        return rows.stream().findFirst();
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
                select mr.id as review_id, mr.user_id, u.name as writer_name,
                       mr.cafeteria_id, mr.menu_id, mr.meal_menu_id, mr.meal_date,
                       mr.content, mr.like_count, mr.comment_count, mr.created_at, mr.updated_at
                from menu_review mr
                join users u on u.id = mr.user_id
                where mr.id = :reviewId
                  and mr.deleted_at is null
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("reviewId", reviewId);
        List<MenuReviewRow> rows = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MenuReviewRow(
                rs.getLong("review_id"),
                rs.getLong("user_id"),
                rs.getString("writer_name"),
                rs.getLong("cafeteria_id"),
                rs.getLong("menu_id"),
                rs.getObject("meal_menu_id", Long.class),
                rs.getObject("meal_date", LocalDate.class),
                rs.getString("content"),
                rs.getLong("like_count"),
                rs.getLong("comment_count"),
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
                select cafeteria_id, menu_id, count(*) as review_count
                from menu_review
                where deleted_at is null
                  and cafeteria_id in (:cafeteriaIds)
                  and menu_id in (:menuIds)
                group by cafeteria_id, menu_id
                """;
        List<Long> cafeteriaIds = targets.stream().map(MenuLikeTarget::cafeteriaId).distinct().toList();
        List<Long> menuIds = targets.stream().map(MenuLikeTarget::menuId).distinct().toList();
        Map<MenuLikeTarget, Long> map = new HashMap<>();
        namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("cafeteriaIds", cafeteriaIds)
                .addValue("menuIds", menuIds), rs -> {
            MenuLikeTarget key = new MenuLikeTarget(rs.getLong("cafeteria_id"), rs.getLong("menu_id"));
            if (targets.contains(key)) {
                map.put(key, rs.getLong("review_count"));
            }
        });
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findParticipantUserIdsByMenuTarget(Long cafeteriaId, Long menuId) {
        String sql = """
                select distinct participant.user_id
                from (
                    select mr.user_id
                    from menu_review mr
                    where mr.cafeteria_id = :cafeteriaId
                      and mr.menu_id = :menuId
                      and mr.deleted_at is null

                    union

                    select mrc.user_id
                    from menu_review_comment mrc
                    join menu_review mr on mr.id = mrc.review_id
                    where mr.cafeteria_id = :cafeteriaId
                      and mr.menu_id = :menuId
                      and mr.deleted_at is null
                      and mrc.deleted_at is null
                ) participant
                order by participant.user_id asc
                """;
        return namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId), (rs, rowNum) -> rs.getLong("user_id"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuReviewRow> findReviewPage(Long cafeteriaId, Long menuId, int page, int size) {
        String sql = """
                select mr.id as review_id, mr.user_id, u.name as writer_name,
                       mr.cafeteria_id, mr.menu_id, mr.meal_menu_id, mr.meal_date,
                       mr.content, mr.like_count, mr.comment_count, mr.created_at, mr.updated_at
                from menu_review mr
                join users u on u.id = mr.user_id
                where mr.cafeteria_id = :cafeteriaId
                  and mr.menu_id = :menuId
                  and mr.deleted_at is null
                order by mr.meal_date desc nulls last, mr.like_count desc, mr.created_at desc, mr.id desc
                limit :size offset :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cafeteriaId", cafeteriaId)
                .addValue("menuId", menuId)
                .addValue("size", size)
                .addValue("offset", (long) page * size);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MenuReviewRow(
                rs.getLong("review_id"),
                rs.getLong("user_id"),
                rs.getString("writer_name"),
                rs.getLong("cafeteria_id"),
                rs.getLong("menu_id"),
                rs.getObject("meal_menu_id", Long.class),
                rs.getObject("meal_date", LocalDate.class),
                rs.getString("content"),
                rs.getLong("like_count"),
                rs.getLong("comment_count"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findLikedReviewIds(Long userId, List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return Set.of();
        }
        String sql = """
                select review_id
                from menu_review_like
                where user_id = :userId
                  and review_id in (:reviewIds)
                """;
        List<Long> likedIdRows = namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("reviewIds", reviewIds), (rs, rowNum) -> rs.getLong("review_id"));
        Set<Long> likedIds = new HashSet<>(likedIdRows);
        return likedIds;
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
        menuReviewLikeJpaRepository.save(MenuReviewLike.create(reviewId, userId));
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
    public List<MenuReviewCommentRow> findCommentPage(Long reviewId, int page, int size) {
        String sql = """
                select c.id as comment_id, c.review_id, c.user_id, u.name as writer_name,
                       c.content, c.created_at, c.updated_at
                from menu_review_comment c
                join users u on u.id = c.user_id
                where c.review_id = :reviewId
                  and c.deleted_at is null
                order by c.created_at asc, c.id asc
                limit :size offset :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("reviewId", reviewId)
                .addValue("size", size)
                .addValue("offset", (long) page * size);
        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> new MenuReviewCommentRow(
                rs.getLong("comment_id"),
                rs.getLong("review_id"),
                rs.getLong("user_id"),
                rs.getString("writer_name"),
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
                select c.id as comment_id, c.review_id, c.user_id, u.name as writer_name,
                       c.content, c.created_at, c.updated_at
                from menu_review_comment c
                join users u on u.id = c.user_id
                where c.id = :commentId
                  and c.deleted_at is null
                """;
        List<MenuReviewCommentRow> rows = namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource("commentId", commentId),
                (rs, rowNum) -> new MenuReviewCommentRow(
                        rs.getLong("comment_id"),
                        rs.getLong("review_id"),
                        rs.getLong("user_id"),
                        rs.getString("writer_name"),
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
