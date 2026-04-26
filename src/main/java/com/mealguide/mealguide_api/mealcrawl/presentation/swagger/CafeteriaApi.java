package com.mealguide.mealguide_api.mealcrawl.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.CafeteriaListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

@SecurityRequirement(name = "Access Token")
public interface CafeteriaApi {

    @Operation(
            summary = "현재 사용자 학교 식당 목록 조회",
            description = "인증된 사용자 schoolId 기준으로 해당 학교의 식당 목록을 조회합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = CafeteriaListResponse.class, description = "식당 목록 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR)
            }
    )
    ResponseEntity<ResponseBody<CafeteriaListResponse>> getCafeterias(
            @CurrentUserId Long currentUserId
    );
}
