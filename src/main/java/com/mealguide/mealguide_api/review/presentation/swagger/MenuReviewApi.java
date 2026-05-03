package com.mealguide.mealguide_api.review.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.review.presentation.dto.request.CreateMenuReviewRequest;
import com.mealguide.mealguide_api.review.presentation.dto.request.CreateReviewCommentRequest;
import com.mealguide.mealguide_api.review.presentation.dto.request.UpdateMenuReviewRequest;
import com.mealguide.mealguide_api.review.presentation.dto.request.UpdateReviewCommentRequest;
import com.mealguide.mealguide_api.review.presentation.dto.response.MenuReviewListResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.MenuReviewResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewCommentListResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewCommentResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.ReviewLikeToggleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@SecurityRequirement(name = "Access Token")
public interface MenuReviewApi {

    @Operation(
            summary = "메뉴 리뷰 목록 조회",
            description = "mealMenuId를 cafeteriaId+menuId로 변환해 커뮤니티형 리뷰를 페이지 단위로 조회합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = MenuReviewListResponse.class, description = "메뉴 리뷰 목록 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.MEAL_MENU_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR)
            }
    )
    ResponseEntity<ResponseBody<MenuReviewListResponse>> getReviews(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    );

    @Operation(
            summary = "메뉴 리뷰 작성",
            description = "사용자가 메뉴 리뷰 글을 작성합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = MenuReviewResponse.class, description = "메뉴 리뷰 작성 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.MEAL_MENU_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_REVIEW_CONTENT)
            }
    )
    ResponseEntity<ResponseBody<MenuReviewResponse>> createReview(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId,
            @Valid @RequestBody CreateMenuReviewRequest request
    );

    @Operation(
            summary = "메뉴 리뷰 수정",
            description = "본인 메뉴 리뷰 글을 수정합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = MenuReviewResponse.class, description = "메뉴 리뷰 수정 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.MEAL_MENU_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.REVIEW_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.REVIEW_FORBIDDEN),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_REVIEW_CONTENT)
            }
    )
    ResponseEntity<ResponseBody<MenuReviewResponse>> updateReview(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateMenuReviewRequest request
    );

    @Operation(
            summary = "메뉴 리뷰 삭제",
            description = "본인 메뉴 리뷰 글을 soft delete 합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = Void.class, description = "메뉴 리뷰 삭제 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.MEAL_MENU_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.REVIEW_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.REVIEW_FORBIDDEN)
            }
    )
    ResponseEntity<ResponseBody<Void>> deleteReview(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId,
            @PathVariable Long reviewId
    );

    @Operation(
            summary = "리뷰 좋아요 토글",
            description = "리뷰 좋아요를 등록/취소하고 최신 상태를 반환합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = ReviewLikeToggleResponse.class, description = "리뷰 좋아요 토글 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.REVIEW_NOT_FOUND)
            }
    )
    ResponseEntity<ResponseBody<ReviewLikeToggleResponse>> toggleReviewLike(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId
    );

    @Operation(
            summary = "리뷰 댓글 목록 조회",
            description = "리뷰의 활성 댓글을 오래된 순으로 페이지 조회합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = ReviewCommentListResponse.class, description = "리뷰 댓글 목록 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.REVIEW_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR)
            }
    )
    ResponseEntity<ResponseBody<ReviewCommentListResponse>> getComments(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    );

    @Operation(
            summary = "리뷰 댓글 작성",
            description = "리뷰에 댓글을 작성합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = ReviewCommentResponse.class, description = "리뷰 댓글 작성 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.REVIEW_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_COMMENT_CONTENT)
            }
    )
    ResponseEntity<ResponseBody<ReviewCommentResponse>> createComment(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReviewCommentRequest request
    );

    @Operation(
            summary = "리뷰 댓글 수정",
            description = "본인 댓글을 수정합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = ReviewCommentResponse.class, description = "리뷰 댓글 수정 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.REVIEW_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.COMMENT_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.COMMENT_FORBIDDEN),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_COMMENT_CONTENT)
            }
    )
    ResponseEntity<ResponseBody<ReviewCommentResponse>> updateComment(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateReviewCommentRequest request
    );

    @Operation(
            summary = "리뷰 댓글 삭제",
            description = "본인 댓글을 soft delete 합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = Void.class, description = "리뷰 댓글 삭제 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.REVIEW_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.COMMENT_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.COMMENT_FORBIDDEN)
            }
    )
    ResponseEntity<ResponseBody<Void>> deleteComment(
            @CurrentUserId Long currentUserId,
            @PathVariable Long reviewId,
            @PathVariable Long commentId
    );
}
