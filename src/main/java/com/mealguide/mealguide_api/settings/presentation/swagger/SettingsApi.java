package com.mealguide.mealguide_api.settings.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateAllergiesRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateLanguageRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateReligionRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@SecurityRequirement(name = "Access Token")
public interface SettingsApi {

    @Operation(
            summary = "내 언어 설정 조회",
            description = "인증된 사용자의 현재 언어 코드 설정을 조회합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = LanguageUpdateResponse.class, description = "언어 설정 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND)
            }
    )
    ResponseEntity<ResponseBody<LanguageUpdateResponse>> getLanguage(@CurrentUserId Long currentUserId);

    @Operation(
            summary = "내 알레르기 설정 조회",
            description = "인증된 사용자의 현재 알레르기 코드 설정을 조회합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = AllergyUpdateResponse.class, description = "알레르기 설정 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND)
            }
    )
    ResponseEntity<ResponseBody<AllergyUpdateResponse>> getAllergies(@CurrentUserId Long currentUserId);

    @Operation(
            summary = "내 종교별 식이 제한 설정 조회",
            description = "인증된 사용자의 현재 종교별 식이 제한 코드를 조회합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = ReligionUpdateResponse.class, description = "종교별 식이 제한 설정 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND)
            }
    )
    ResponseEntity<ResponseBody<ReligionUpdateResponse>> getReligion(@CurrentUserId Long currentUserId);

    @Operation(
            summary = "언어 설정 변경",
            description = "인증된 사용자의 언어 설정을 변경합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = LanguageUpdateResponse.class, description = "언어 설정 변경 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_LANGUAGE_CODE)
            }
    )
    ResponseEntity<ResponseBody<LanguageUpdateResponse>> updateLanguage(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody UpdateLanguageRequest request
    );

    @Operation(
            summary = "알레르기 설정 변경",
            description = "인증된 사용자의 알레르기 선택 목록을 전체 교체합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = AllergyUpdateResponse.class, description = "알레르기 설정 변경 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_ALLERGY_CODE)
            }
    )
    ResponseEntity<ResponseBody<AllergyUpdateResponse>> updateAllergies(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody UpdateAllergiesRequest request
    );

    @Operation(
            summary = "종교별 식이 제한 설정 변경",
            description = "인증된 사용자의 종교별 식이 제한 설정을 변경합니다. religiousCode가 null이면 설정을 해제합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = ReligionUpdateResponse.class, description = "종교별 식이 제한 설정 변경 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ErrorCode.INVALID_RELIGIOUS_CODE)
            }
    )
    ResponseEntity<ResponseBody<ReligionUpdateResponse>> updateReligion(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody UpdateReligionRequest request
    );
}
