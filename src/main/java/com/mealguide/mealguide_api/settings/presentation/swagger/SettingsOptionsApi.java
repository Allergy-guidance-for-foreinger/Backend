package com.mealguide.mealguide_api.settings.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;

@SecurityRequirement(name = "Access Token")
public interface SettingsOptionsApi {

    @Operation(
            summary = "언어 전체 선택지 조회",
            description = "설정 화면에서 사용하는 전체 언어 선택지 목록을 조회합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = LanguageOptionsResponse.class, description = "언어 선택지 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED)
            }
    )
    ResponseEntity<ResponseBody<LanguageOptionsResponse>> getLanguageOptions();

    @Operation(
            summary = "알레르기 전체 선택지 조회",
            description = "인증된 사용자의 언어 설정 기준으로 현지화된 알레르기 선택지 목록을 조회합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = AllergyOptionsResponse.class, description = "알레르기 선택지 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND)
            }
    )
    ResponseEntity<ResponseBody<AllergyOptionsResponse>> getAllergyOptions(@CurrentUserId Long currentUserId);

    @Operation(
            summary = "종교적 식이 제한 전체 선택지 조회",
            description = "인증된 사용자의 언어 설정 기준으로 현지화된 종교적 식이 제한 선택지 목록을 조회합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = ReligionOptionsResponse.class, description = "종교적 식이 제한 선택지 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND)
            }
    )
    ResponseEntity<ResponseBody<ReligionOptionsResponse>> getReligionOptions(@CurrentUserId Long currentUserId);
}

