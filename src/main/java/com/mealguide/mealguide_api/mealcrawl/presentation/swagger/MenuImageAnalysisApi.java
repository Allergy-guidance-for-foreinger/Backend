package com.mealguide.mealguide_api.mealcrawl.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuImageAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuImageAnalysisUsageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@SecurityRequirement(name = "Access Token")
public interface MenuImageAnalysisApi {
    @Operation(summary = "음식 사진 분석", description = "음식 사진을 업로드해 식별/분석 결과를 반환합니다.")
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = MenuImageAnalysisResponse.class, description = "분석 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR),
                    @SwaggerApiFailedResponse(ErrorCode.MENU_IMAGE_ANALYSIS_LIMIT_EXCEEDED),
                    @SwaggerApiFailedResponse(ErrorCode.UNEXPECTED_SERVER_ERROR)
            }
    )
    ResponseEntity<ResponseBody<MenuImageAnalysisResponse>> analyzeImage(
            @CurrentUserId Long currentUserId,
            MultipartFile image
    );

    @Operation(summary = "음식 사진 분석 사용량 조회", description = "오늘 음식 사진 분석 사용 횟수와 남은 횟수를 반환합니다.")
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = MenuImageAnalysisUsageResponse.class, description = "사용량 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.UNEXPECTED_SERVER_ERROR)
            }
    )
    ResponseEntity<ResponseBody<MenuImageAnalysisUsageResponse>> getUsage(
            @CurrentUserId Long currentUserId
    );
}
