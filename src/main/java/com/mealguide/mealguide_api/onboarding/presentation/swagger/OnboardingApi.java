package com.mealguide.mealguide_api.onboarding.presentation.swagger;

import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.onboarding.presentation.dto.response.SchoolListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface OnboardingApi {

    @SecurityRequirements
    @Operation(
            summary = "온보딩 학교 목록 조회",
            description = "온보딩 화면에서 선택할 수 있는 학교 목록을 조회합니다. lang이 있으면 번역명을 우선 사용하고 없으면 기본 학교명을 반환합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = SchoolListResponse.class, description = "학교 목록 조회 성공")
    )
    ResponseEntity<ResponseBody<SchoolListResponse>> getSchools(@RequestParam(required = false) String lang);
}
