package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewCommentRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewRow;
import com.mealguide.mealguide_api.review.application.dto.MenuReviewTargetRow;
import com.mealguide.mealguide_api.review.application.port.MenuReviewPort;
import com.mealguide.mealguide_api.review.application.service.MenuReviewService;
import com.mealguide.mealguide_api.review.presentation.dto.response.MenuReviewListResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewCommentListResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewLikeToggleResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuReviewServiceTest {

    @Test
    void createReviewAllowsSameUserMultiplePosts() {
        MenuReviewPort port = mock(MenuReviewPort.class);
        MenuReviewService service = new MenuReviewService(port);
        when(port.findTargetByMealMenuId(10L)).thenReturn(Optional.of(new MenuReviewTargetRow(10L, 1L, 25L, LocalDate.now())));
        when(port.findAnonymousNamesByMenuTargetAndUserIds(1L, 25L, Set.of(1L)))
                .thenReturn(java.util.Map.of(1L, "Anonymous 1"));
        when(port.saveReview(1L, 1L, 25L, 10L, LocalDate.now(), "first")).thenReturn(100L);
        when(port.findActiveReviewById(100L)).thenReturn(Optional.of(reviewRow(100L, 1L, 1L, 25L, "first", 0, 0)));

        service.createReview(1L, 10L, "first");
        when(port.saveReview(1L, 1L, 25L, 10L, LocalDate.now(), "second")).thenReturn(101L);
        when(port.findActiveReviewById(101L)).thenReturn(Optional.of(reviewRow(101L, 1L, 1L, 25L, "second", 0, 0)));
        service.createReview(1L, 10L, "second");

        verify(port, times(2)).ensureAnonymousParticipant(1L, 25L, 1L);
        verify(port).saveReview(1L, 1L, 25L, 10L, LocalDate.now(), "first");
        verify(port).saveReview(1L, 1L, 25L, 10L, LocalDate.now(), "second");
    }

    @Test
    void listUsesCafeteriaMenuTargetAndPagination() {
        MenuReviewPort port = mock(MenuReviewPort.class);
        MenuReviewService service = new MenuReviewService(port);
        when(port.findTargetByMealMenuId(10L)).thenReturn(Optional.of(new MenuReviewTargetRow(10L, 1L, 25L, LocalDate.now())));
        when(port.countActiveReviews(1L, 25L)).thenReturn(2L);
        when(port.findReviewPage(1L, 1L, 25L, 0, 20)).thenReturn(List.of(
                reviewRow(101L, 2L, 1L, 25L, "a", 3, 1, true, 1L),
                reviewRow(100L, 3L, 1L, 25L, "b", 1, 0, false, 2L)
        ));

        MenuReviewListResponse response = service.getReviews(1L, 10L, 0, 20);

        assertThat(response.cafeteriaId()).isEqualTo(1L);
        assertThat(response.menuId()).isEqualTo(25L);
        assertThat(response.reviewCount()).isEqualTo(2L);
        assertThat(response.reviews()).hasSize(2);
        assertThat(response.reviews().get(0).likedByMe()).isTrue();
        assertThat(response.reviews().get(0).writerName()).isEqualTo("Anonymous 1");
        assertThat(response.reviews().get(1).writerName()).isEqualTo("Anonymous 2");
    }

    @Test
    void listShowsDeletedUserForDeletedWriter() {
        MenuReviewPort port = mock(MenuReviewPort.class);
        MenuReviewService service = new MenuReviewService(port);
        when(port.findTargetByMealMenuId(10L)).thenReturn(Optional.of(new MenuReviewTargetRow(10L, 1L, 25L, LocalDate.now())));
        when(port.countActiveReviews(1L, 25L)).thenReturn(2L);
        when(port.findReviewPage(1L, 1L, 25L, 0, 20)).thenReturn(List.of(
                reviewRow(101L, 2L, 1L, 25L, "active", 0, 0, false, 1L),
                deletedReviewRow(100L, 1L, 25L, "deleted")
        ));

        MenuReviewListResponse response = service.getReviews(1L, 10L, 0, 20);

        assertThat(response.reviews()).hasSize(2);
        assertThat(response.reviews().get(0).writerName()).isEqualTo("Anonymous 1");
        assertThat(response.reviews().get(1).writerName()).isEqualTo("Deleted user");
        assertThat(response.reviews().get(1).mine()).isFalse();
    }

    @Test
    void toggleReviewLikeReturnsLatestState() {
        MenuReviewPort port = mock(MenuReviewPort.class);
        MenuReviewService service = new MenuReviewService(port);
        when(port.findActiveReviewById(10L)).thenReturn(Optional.of(reviewRow(10L, 2L, 1L, 25L, "c", 0, 0)));
        when(port.findAnonymousNamesByMenuTargetAndUserIds(1L, 25L, Set.of(2L)))
                .thenReturn(java.util.Map.of(2L, "Anonymous 2"));
        when(port.existsReviewLike(10L, 1L)).thenReturn(false, true);
        when(port.findReviewLikeCount(10L)).thenReturn(Optional.of(1L));

        ReviewLikeToggleResponse response = service.toggleReviewLike(1L, 10L);

        verify(port).saveReviewLike(10L, 1L);
        verify(port).incrementReviewLikeCount(10L);
        assertThat(response.likedByMe()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1L);
    }

    @Test
    void updateReviewDeniedForOtherUser() {
        MenuReviewPort port = mock(MenuReviewPort.class);
        MenuReviewService service = new MenuReviewService(port);
        when(port.findTargetByMealMenuId(10L)).thenReturn(Optional.of(new MenuReviewTargetRow(10L, 1L, 25L, LocalDate.now())));
        when(port.findActiveReviewById(99L)).thenReturn(Optional.of(reviewRow(99L, 2L, 1L, 25L, "x", 0, 0)));
        when(port.findAnonymousNamesByMenuTargetAndUserIds(1L, 25L, Set.of(2L)))
                .thenReturn(java.util.Map.of(2L, "Anonymous 2"));

        assertThatThrownBy(() -> service.updateReview(1L, 10L, 99L, "new"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void commentCreateAndDeleteAdjustCount() {
        MenuReviewPort port = mock(MenuReviewPort.class);
        MenuReviewService service = new MenuReviewService(port);
        when(port.findActiveReviewById(10L)).thenReturn(Optional.of(reviewRow(10L, 2L, 1L, 25L, "c", 0, 0)));
        when(port.findAnonymousNamesByMenuTargetAndUserIds(1L, 25L, Set.of(1L)))
                .thenReturn(java.util.Map.of(1L, "Anonymous 1"));
        when(port.saveComment(10L, 1L, "hello")).thenReturn(5L);
        when(port.findActiveCommentById(5L)).thenReturn(Optional.of(new MenuReviewCommentRow(
                5L, 10L, 1L, "writer", false, null, "hello", LocalDateTime.now(), LocalDateTime.now()
        )));

        service.createComment(1L, 10L, "hello");
        verify(port).ensureAnonymousParticipant(1L, 25L, 1L);
        verify(port).incrementReviewCommentCount(10L);

        when(port.findActiveCommentById(5L)).thenReturn(Optional.of(new MenuReviewCommentRow(
                5L, 10L, 1L, "writer", false, null, "hello", LocalDateTime.now(), LocalDateTime.now()
        )));
        service.deleteComment(1L, 10L, 5L);
        verify(port).decrementReviewCommentCount(10L);
    }

    @Test
    void commentListUsesAnonymousNumberFromPageRows() {
        MenuReviewPort port = mock(MenuReviewPort.class);
        MenuReviewService service = new MenuReviewService(port);
        when(port.findActiveReviewById(10L)).thenReturn(Optional.of(reviewRow(10L, 2L, 1L, 25L, "c", 0, 2)));
        when(port.findCommentPage(10L, 1L, 25L, 0, 20)).thenReturn(List.of(
                new MenuReviewCommentRow(5L, 10L, 1L, null, false, 1L, "hello", LocalDateTime.now(), LocalDateTime.now()),
                new MenuReviewCommentRow(6L, 10L, null, null, true, null, "withdrawn", LocalDateTime.now(), LocalDateTime.now())
        ));
        when(port.countActiveComments(10L)).thenReturn(2L);

        ReviewCommentListResponse response = service.getComments(1L, 10L, 0, 20);

        assertThat(response.comments()).hasSize(2);
        assertThat(response.comments().get(0).writerName()).isEqualTo("Anonymous 1");
        assertThat(response.comments().get(0).mine()).isTrue();
        assertThat(response.comments().get(1).writerName()).isEqualTo("Deleted user");
        assertThat(response.comments().get(1).mine()).isFalse();
        verify(port).findCommentPage(10L, 1L, 25L, 0, 20);
    }

    private MenuReviewRow reviewRow(Long reviewId, Long userId, Long cafeteriaId, Long menuId, String content, long like, long comment) {
        return reviewRow(reviewId, userId, cafeteriaId, menuId, content, like, comment, false, null);
    }

    private MenuReviewRow reviewRow(
            Long reviewId,
            Long userId,
            Long cafeteriaId,
            Long menuId,
            String content,
            long like,
            long comment,
            boolean likedByMe,
            Long anonymousNo
    ) {
        return new MenuReviewRow(
                reviewId,
                userId,
                "writer",
                false,
                cafeteriaId,
                menuId,
                10L,
                LocalDate.now(),
                content,
                like,
                comment,
                likedByMe,
                anonymousNo,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private MenuReviewRow deletedReviewRow(Long reviewId, Long cafeteriaId, Long menuId, String content) {
        return new MenuReviewRow(
                reviewId,
                null,
                null,
                true,
                cafeteriaId,
                menuId,
                10L,
                LocalDate.now(),
                content,
                0,
                0,
                false,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
