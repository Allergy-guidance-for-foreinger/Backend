package com.mealguide.mealguide_api.mealcrawl.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.WeeklyMealResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@SecurityRequirement(name = "Access Token")
public interface WeeklyMealApi {

    @Operation(
            summary = "사용자 주간 식단 조회",
            description = "Redis 캐시 기반 주간 식단을 조회하고, 현재 사용자 설정 기준 메뉴 위험도를 계산해 반환합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = WeeklyMealResponse.class, description = "주간 식단 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR)
            }
    )
    ResponseEntity<ResponseBody<WeeklyMealResponse>> getWeeklyMeals(
            @CurrentUserId Long currentUserId,
            @RequestParam Long cafeteriaId,
            @RequestParam LocalDate weekStartDate
    );
}
