package com.mealguide.mealguide_api.review.application.port;

import com.mealguide.mealguide_api.review.application.dto.MenuReviewCommentRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewTargetRow;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MenuReviewPort {

    Optional<MenuReviewTargetRow> findTargetByMealMenuId(Long mealMenuId);

    void ensureAnonymousParticipant(Long cafeteriaId, Long menuId, Long userId);

    Long saveReview(Long userId, Long cafeteriaId, Long menuId, Long mealMenuId, java.time.LocalDate mealDate, String content);

    Optional<MenuReviewRow> findActiveReviewById(Long reviewId);

    void updateReviewContent(Long reviewId, String content);

    void softDeleteReview(Long reviewId);

    long countActiveReviews(Long cafeteriaId, Long menuId);

    Map<MenuLikeTarget, Long> countActiveReviewsByTargets(Set<MenuLikeTarget> targets);

    Map<Long, String> findAnonymousNamesByMenuTargetAndUserIds(Long cafeteriaId, Long menuId, Set<Long> userIds);

    List<MenuReviewRow> findReviewPage(Long userId, Long cafeteriaId, Long menuId, int page, int size);

    boolean existsReviewLike(Long reviewId, Long userId);

    void saveReviewLike(Long reviewId, Long userId);

    void deleteReviewLike(Long reviewId, Long userId);

    void incrementReviewLikeCount(Long reviewId);

    void decrementReviewLikeCount(Long reviewId);

    Optional<Long> findReviewLikeCount(Long reviewId);

    List<MenuReviewCommentRow> findCommentPage(Long reviewId, Long cafeteriaId, Long menuId, int page, int size);

    long countActiveComments(Long reviewId);

    Long saveComment(Long reviewId, Long userId, String content);

    Optional<MenuReviewCommentRow> findActiveCommentById(Long commentId);

    void updateCommentContent(Long commentId, String content);

    void softDeleteComment(Long commentId);

    void incrementReviewCommentCount(Long reviewId);

    void decrementReviewCommentCount(Long reviewId);
}
