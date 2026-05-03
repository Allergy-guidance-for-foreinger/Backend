package com.mealguide.mealguide_api.review.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.review.application.service.MenuReviewService;
import com.mealguide.mealguide_api.review.presentation.dto.request.CreateMenuReviewRequest;
import com.mealguide.mealguide_api.review.presentation.dto.request.CreateReviewCommentRequest;
import com.mealguide.mealguide_api.review.presentation.dto.request.UpdateMenuReviewRequest;
import com.mealguide.mealguide_api.review.presentation.dto.request.UpdateReviewCommentRequest;
import com.mealguide.mealguide_api.review.presentation.dto.response.MenuReviewListResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.MenuReviewResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewCommentListResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewCommentResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewLikeToggleResponse;
import com.mealguide.mealguide_api.review.presentation.swagger.MenuReviewApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1")
public class MenuReviewController implements MenuReviewApi {

    private final MenuReviewService menuReviewService;

    @GetMapping("/meal-menus/{mealMenuId}/reviews")
    public ResponseEntity<ResponseBody<MenuReviewListResponse>> getReviews(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                menuReviewService.getReviews(currentUserId, mealMenuId, page, size)
        ));
    }

    @PostMapping("/meal-menus/{mealMenuId}/reviews")
    public ResponseEntity<ResponseBody<MenuReviewResponse>> createReview(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId,
            @Valid @RequestBody CreateMenuReviewRequest request
    ) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                menuReviewService.createReview(currentUserId, mealMenuId, request.content())
        ));
    }

    @PatchMapping("/meal-menus/{mealMenuId}/reviews/{reviewId}")
    public ResponseEntity<ResponseBody<MenuReviewResponse>> updateReview(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateMenuReviewRequest request
    ) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                menuReviewService.updateReview(currentUserId, mealMenuId, reviewId, request.content())
        ));
    }

    @DeleteMapping("/meal-menus/{mealMenuId}/reviews/{reviewId}")
    public ResponseEntity<ResponseBody<Void>> deleteReview(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId,
            @PathVariable Long reviewId
    ) {
        menuReviewService.deleteReview(currentUserId, mealMenuId, reviewId);
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse());
    }

    @PostMapping("/reviews/{reviewId}/like")
    public ResponseEntity<ResponseBody<ReviewLikeToggleResponse>> toggleReviewLike(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                menuReviewService.toggleReviewLike(currentUserId, reviewId)
        ));
    }

    @GetMapping("/reviews/{reviewId}/comments")
    public ResponseEntity<ResponseBody<ReviewCommentListResponse>> getComments(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                menuReviewService.getComments(currentUserId, reviewId, page, size)
        ));
    }

    @PostMapping("/reviews/{reviewId}/comments")
    public ResponseEntity<ResponseBody<ReviewCommentResponse>> createComment(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReviewCommentRequest request
    ) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                menuReviewService.createComment(currentUserId, reviewId, request.content())
        ));
    }

    @PatchMapping("/reviews/{reviewId}/comments/{commentId}")
    public ResponseEntity<ResponseBody<ReviewCommentResponse>> updateComment(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateReviewCommentRequest request
    ) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                menuReviewService.updateComment(currentUserId, reviewId, commentId, request.content())
        ));
    }

    @DeleteMapping("/reviews/{reviewId}/comments/{commentId}")
    public ResponseEntity<ResponseBody<Void>> deleteComment(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId,
            @PathVariable Long commentId
    ) {
        menuReviewService.deleteComment(currentUserId, reviewId, commentId);
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse());
    }
}
