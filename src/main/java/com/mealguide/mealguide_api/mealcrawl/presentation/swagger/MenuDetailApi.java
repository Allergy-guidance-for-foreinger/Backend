package com.mealguide.mealguide_api.mealcrawl.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@SecurityRequirement(name = "Access Token")
public interface MenuDetailApi {

    @Operation(
            summary = "메뉴 상세 조회",
            description = "mealMenuId 기준으로 메뉴 상세를 조회합니다. matchedAllergies는 현재 사용자 알레르기와 메뉴 재료가 겹친 목록입니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = MenuDetailResponse.class, description = "메뉴 상세 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR),
                    @SwaggerApiFailedResponse(ErrorCode.ESSENTIAL_FIELD_MISSING_ERROR)
            }
    )
    ResponseEntity<ResponseBody<MenuDetailResponse>> getMenuDetail(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId
    );
}
