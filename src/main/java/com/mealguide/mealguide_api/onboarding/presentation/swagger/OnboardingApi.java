package com.mealguide.mealguide_api.onboarding.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.onboarding.presentation.dto.request.CompleteOnboardingRequest;
import com.mealguide.mealguide_api.onboarding.presentation.dto.response.CompleteOnboardingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface OnboardingApi {

    @SecurityRequirement(name = "Access Token")
    @Operation(
            summary = "온보딩 정보 저장",
            description = "언어, 학교, 알레르기, 종교 식이 제한, 국가 정보를 한 번에 저장하고 온보딩 완료 상태로 변경합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = CompleteOnboardingResponse.class, description = "온보딩 저장 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_LANGUAGE_CODE),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_ALLERGY_CODE),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_RELIGIOUS_CODE),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_COUNTRY_CODE),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR)
            }
    )
    ResponseEntity<ResponseBody<CompleteOnboardingResponse>> completeOnboarding(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody CompleteOnboardingRequest request
    );
}
