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
            summary = "Get onboarding school list",
            description = "Returns selectable schools for onboarding. If lang is provided, translated names are preferred."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = SchoolListResponse.class, description = "School list fetched")
    )
    ResponseEntity<ResponseBody<SchoolListResponse>> getSchools(@RequestParam(required = false) String lang);

    @SecurityRequirement(name = "Access Token")
    @Operation(
            summary = "Save onboarding profile",
            description = "Saves language, school, allergy, and religious preference in one request and marks onboarding as completed."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = CompleteOnboardingResponse.class, description = "Onboarding saved"),
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
