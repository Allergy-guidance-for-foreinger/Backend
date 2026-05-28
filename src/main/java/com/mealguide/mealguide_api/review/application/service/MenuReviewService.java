package com.mealguide.mealguide_api.review.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewCommentRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewTargetRow;
import com.mealguide.mealguide_api.review.application.port.MenuReviewPort;
import com.mealguide.mealguide_api.review.presentation.dto.response.MenuReviewItemResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.MenuReviewListResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.MenuReviewResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.PageInfoResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewCommentItemResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewCommentListResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewCommentResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewLikeToggleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MenuReviewService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int MAX_REVIEW_CONTENT_LENGTH = 500;
    private static final int MAX_COMMENT_CONTENT_LENGTH = 300;
    private static final String ANONYMOUS_FALLBACK_NAME = "Anonymous";
    private static final String DELETED_USER_NAME = "Deleted user";

    private final MenuReviewPort menuReviewPort;

    @Transactional(readOnly = true)
    public MenuReviewListResponse getReviews(Long userId, Long mealMenuId, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        MenuReviewTargetRow target = menuReviewPort.findTargetByMealMenuId(mealMenuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEAL_MENU_NOT_FOUND));

        long total = menuReviewPort.countActiveReviews(target.cafeteriaId(), target.menuId());
        List<MenuReviewRow> rows = menuReviewPort.findReviewPage(target.cafeteriaId(), target.menuId(), normalizedPage, normalizedSize);
        List<Long> reviewIds = rows.stream().map(MenuReviewRow::reviewId).toList();
        Set<Long> likedIds = menuReviewPort.findLikedReviewIds(userId, reviewIds);
        Set<Long> writerUserIds = rows.stream()
                .filter(row -> !row.writerDeleted())
                .map(MenuReviewRow::userId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> anonymousNames = resolveAnonymousNamesForUsers(target.cafeteriaId(), target.menuId(), writerUserIds);

        List<MenuReviewItemResponse> items = rows.stream().map(row -> new MenuReviewItemResponse(
                row.reviewId(),
                resolveWriterName(row.userId(), row.writerDeleted(), anonymousNames),
                row.content(),
                row.mealDate(),
                row.likeCount(),
                row.commentCount(),
                likedIds.contains(row.reviewId()),
                isMine(userId, row.userId(), row.writerDeleted()),
                row.createdAt(),
                row.updatedAt()
        )).toList();

        return new MenuReviewListResponse(
                mealMenuId,
                target.cafeteriaId(),
                target.menuId(),
                total,
                items,
                toPageInfo(normalizedPage, normalizedSize, total)
        );
    }

    @Transactional
    public MenuReviewResponse createReview(Long userId, Long mealMenuId, String content) {
        String normalizedContent = normalizeReviewContent(content);
        MenuReviewTargetRow target = menuReviewPort.findTargetByMealMenuId(mealMenuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEAL_MENU_NOT_FOUND));
        menuReviewPort.ensureAnonymousParticipant(target.cafeteriaId(), target.menuId(), userId);
        Long reviewId = menuReviewPort.saveReview(
                userId,
                target.cafeteriaId(),
                target.menuId(),
                target.mealMenuId(),
                target.mealDate(),
                normalizedContent
        );
        MenuReviewRow review = menuReviewPort.findActiveReviewById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        Map<Long, String> anonymousNames = resolveAnonymousNamesForUsers(
                review.cafeteriaId(),
                review.menuId(),
                Set.of(review.userId())
        );
        return toReviewResponse(review, false, true, anonymousNames);
    }

    @Transactional
    public MenuReviewResponse updateReview(Long userId, Long mealMenuId, Long reviewId, String content) {
        String normalizedContent = normalizeReviewContent(content);
        MenuReviewTargetRow target = menuReviewPort.findTargetByMealMenuId(mealMenuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEAL_MENU_NOT_FOUND));
        MenuReviewRow review = menuReviewPort.findActiveReviewById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_NOT_FOUND));
        validateReviewTarget(target, review);
        if (!userId.equals(review.userId())) {
            throw new ServiceException(ErrorCode.REVIEW_FORBIDDEN);
        }

        menuReviewPort.updateReviewContent(reviewId, normalizedContent);
        MenuReviewRow updated = menuReviewPort.findActiveReviewById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        boolean likedByMe = menuReviewPort.existsReviewLike(reviewId, userId);
        Map<Long, String> anonymousNames = resolveAnonymousNamesForUsers(
                updated.cafeteriaId(),
                updated.menuId(),
                Set.of(updated.userId())
        );
        return toReviewResponse(updated, likedByMe, true, anonymousNames);
    }

    @Transactional
    public void deleteReview(Long userId, Long mealMenuId, Long reviewId) {
        MenuReviewTargetRow target = menuReviewPort.findTargetByMealMenuId(mealMenuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEAL_MENU_NOT_FOUND));
        MenuReviewRow review = menuReviewPort.findActiveReviewById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_NOT_FOUND));
        validateReviewTarget(target, review);
        if (!userId.equals(review.userId())) {
            throw new ServiceException(ErrorCode.REVIEW_FORBIDDEN);
        }
        menuReviewPort.softDeleteReview(reviewId);
    }

    @Transactional
    public ReviewLikeToggleResponse toggleReviewLike(Long userId, Long reviewId) {
        menuReviewPort.findActiveReviewById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_NOT_FOUND));

        boolean exists = menuReviewPort.existsReviewLike(reviewId, userId);
        boolean likedByMe;
        if (exists) {
            menuReviewPort.deleteReviewLike(reviewId, userId);
            menuReviewPort.decrementReviewLikeCount(reviewId);
            likedByMe = false;
        } else {
            try {
                menuReviewPort.saveReviewLike(reviewId, userId);
                menuReviewPort.incrementReviewLikeCount(reviewId);
                likedByMe = true;
            } catch (DataIntegrityViolationException ignored) {
                likedByMe = true;
            }
        }

        long likeCount = menuReviewPort.findReviewLikeCount(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        return new ReviewLikeToggleResponse(reviewId, likeCount, likedByMe);
    }

    @Transactional(readOnly = true)
    public ReviewCommentListResponse getComments(Long userId, Long reviewId, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        MenuReviewRow review = menuReviewPort.findActiveReviewById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_NOT_FOUND));
        List<MenuReviewCommentRow> commentRows = menuReviewPort.findCommentPage(reviewId, normalizedPage, normalizedSize);
        Set<Long> commentUserIds = commentRows
                .stream()
                .filter(row -> !row.writerDeleted())
                .map(MenuReviewCommentRow::userId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> anonymousNames = resolveAnonymousNamesForUsers(review.cafeteriaId(), review.menuId(), commentUserIds);

        long total = menuReviewPort.countActiveComments(reviewId);
        List<ReviewCommentItemResponse> comments = commentRows
                .stream()
                .map(row -> new ReviewCommentItemResponse(
                        row.commentId(),
                        resolveWriterName(row.userId(), row.writerDeleted(), anonymousNames),
                        row.content(),
                        isMine(userId, row.userId(), row.writerDeleted()),
                        row.createdAt(),
                        row.updatedAt()
                ))
                .toList();
        return new ReviewCommentListResponse(
                reviewId,
                comments,
                toPageInfo(normalizedPage, normalizedSize, total)
        );
    }

    @Transactional
    public ReviewCommentResponse createComment(Long userId, Long reviewId, String content) {
        String normalizedContent = normalizeCommentContent(content);
        MenuReviewRow review = menuReviewPort.findActiveReviewById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_NOT_FOUND));
        menuReviewPort.ensureAnonymousParticipant(review.cafeteriaId(), review.menuId(), userId);
        Long commentId = menuReviewPort.saveComment(reviewId, userId, normalizedContent);
        menuReviewPort.incrementReviewCommentCount(reviewId);
        MenuReviewCommentRow comment = menuReviewPort.findActiveCommentById(commentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.COMMENT_NOT_FOUND));
        Map<Long, String> anonymousNames = resolveAnonymousNamesForUsers(
                review.cafeteriaId(),
                review.menuId(),
                Set.of(comment.userId())
        );
        return toCommentResponse(comment, true, anonymousNames);
    }

    @Transactional
    public ReviewCommentResponse updateComment(Long userId, Long reviewId, Long commentId, String content) {
        String normalizedContent = normalizeCommentContent(content);
        MenuReviewRow review = menuReviewPort.findActiveReviewById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        MenuReviewCommentRow comment = menuReviewPort.findActiveCommentById(commentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        if (!reviewId.equals(comment.reviewId())) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        if (!userId.equals(comment.userId())) {
            throw new ServiceException(ErrorCode.COMMENT_FORBIDDEN);
        }
        menuReviewPort.updateCommentContent(commentId, normalizedContent);
        MenuReviewCommentRow updated = menuReviewPort.findActiveCommentById(commentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));
        Map<Long, String> anonymousNames = resolveAnonymousNamesForUsers(
                review.cafeteriaId(),
                review.menuId(),
                Set.of(updated.userId())
        );
        return toCommentResponse(updated, true, anonymousNames);
    }

    @Transactional
    public void deleteComment(Long userId, Long reviewId, Long commentId) {
        menuReviewPort.findActiveReviewById(reviewId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REVIEW_NOT_FOUND));
        MenuReviewCommentRow comment = menuReviewPort.findActiveCommentById(commentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.COMMENT_NOT_FOUND));
        if (!reviewId.equals(comment.reviewId())) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        if (!userId.equals(comment.userId())) {
            throw new ServiceException(ErrorCode.COMMENT_FORBIDDEN);
        }
        menuReviewPort.softDeleteComment(commentId);
        menuReviewPort.decrementReviewCommentCount(reviewId);
    }

    @Transactional(readOnly = true)
    public long countReviewsByMealMenuId(Long mealMenuId) {
        MenuReviewTargetRow target = menuReviewPort.findTargetByMealMenuId(mealMenuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEAL_MENU_NOT_FOUND));
        return menuReviewPort.countActiveReviews(target.cafeteriaId(), target.menuId());
    }

    private void validateReviewTarget(MenuReviewTargetRow target, MenuReviewRow review) {
        if (!target.cafeteriaId().equals(review.cafeteriaId()) || !target.menuId().equals(review.menuId())) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
    }

    private String normalizeReviewContent(String content) {
        if (content == null) {
            throw new ServiceException(ErrorCode.INVALID_REVIEW_CONTENT);
        }
        String normalized = content.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_REVIEW_CONTENT_LENGTH) {
            throw new ServiceException(ErrorCode.INVALID_REVIEW_CONTENT);
        }
        return normalized;
    }

    private String normalizeCommentContent(String content) {
        if (content == null) {
            throw new ServiceException(ErrorCode.INVALID_COMMENT_CONTENT);
        }
        String normalized = content.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_COMMENT_CONTENT_LENGTH) {
            throw new ServiceException(ErrorCode.INVALID_COMMENT_CONTENT);
        }
        return normalized;
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 0) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size <= 0 || size > MAX_SIZE) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        return size;
    }

    private PageInfoResponse toPageInfo(int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil(totalElements / (double) size);
        boolean hasNext = page + 1 < totalPages;
        return new PageInfoResponse(page, size, totalElements, totalPages, hasNext);
    }

    private MenuReviewResponse toReviewResponse(
            MenuReviewRow review,
            boolean likedByMe,
            boolean mine,
            Map<Long, String> anonymousNames
    ) {
        return new MenuReviewResponse(
                review.reviewId(),
                review.mealMenuId(),
                review.cafeteriaId(),
                review.menuId(),
                resolveWriterName(review.userId(), review.writerDeleted(), anonymousNames),
                review.content(),
                review.mealDate(),
                review.likeCount(),
                review.commentCount(),
                likedByMe,
                isMine(mine, review.userId(), review.writerDeleted()),
                review.createdAt(),
                review.updatedAt()
        );
    }

    private ReviewCommentResponse toCommentResponse(
            MenuReviewCommentRow comment,
            boolean mine,
            Map<Long, String> anonymousNames
    ) {
        return new ReviewCommentResponse(
                comment.commentId(),
                comment.reviewId(),
                resolveWriterName(comment.userId(), comment.writerDeleted(), anonymousNames),
                comment.content(),
                isMine(mine, comment.userId(), comment.writerDeleted()),
                comment.createdAt(),
                comment.updatedAt()
        );
    }

    private String resolveWriterName(Long writerUserId, boolean writerDeleted, Map<Long, String> anonymousNames) {
        if (writerDeleted || writerUserId == null) {
            return DELETED_USER_NAME;
        }
        return anonymousNames.getOrDefault(writerUserId, ANONYMOUS_FALLBACK_NAME);
    }

    private boolean isMine(boolean expectedMine, Long writerUserId, boolean writerDeleted) {
        return expectedMine && writerUserId != null && !writerDeleted;
    }

    private boolean isMine(Long currentUserId, Long writerUserId, boolean writerDeleted) {
        return currentUserId != null && writerUserId != null && !writerDeleted && currentUserId.equals(writerUserId);
    }

    private Map<Long, String> resolveAnonymousNamesForUsers(Long cafeteriaId, Long menuId, Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = menuReviewPort.findAnonymousNamesByMenuTargetAndUserIds(cafeteriaId, menuId, new HashSet<>(userIds));
        return map == null ? Map.of() : map;
    }
}
