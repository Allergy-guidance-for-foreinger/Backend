package com.mealguide.mealguide_api.onboarding.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.onboarding.presentation.dto.request.CompleteOnboardingRequest;
import com.mealguide.mealguide_api.onboarding.presentation.dto.response.CompleteOnboardingResponse;
import com.mealguide.mealguide_api.onboarding.presentation.dto.response.SchoolListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface OnboardingApi {

    @SecurityRequirements
    @Operation(
            summary = "온보딩 학교 목록 조회",
            description = "온보딩 화면에서 선택할 학교 목록을 조회합니다. lang이 있으면 번역명을 우선 사용합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = SchoolListResponse.class, description = "학교 목록 조회 성공")
    )
    ResponseEntity<ResponseBody<SchoolListResponse>> getSchools(@RequestParam(required = false) String lang);

    @SecurityRequirement(name = "Access Token")
    @Operation(
            summary = "온보딩 정보 저장",
            description = "언어, 학교, 알레르기, 종교 식이 제한을 한 번에 저장하고 온보딩 완료 상태로 변경합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = CompleteOnboardingResponse.class, description = "온보딩 저장 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_LANGUAGE_CODE),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_ALLERGY_CODE),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_RELIGIOUS_CODE),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR)
            }
    )
    ResponseEntity<ResponseBody<CompleteOnboardingResponse>> completeOnboarding(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody CompleteOnboardingRequest request
    );
}
