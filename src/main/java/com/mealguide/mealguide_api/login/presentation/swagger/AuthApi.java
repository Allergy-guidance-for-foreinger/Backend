package com.mealguide.mealguide_api.login.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.login.presentation.dto.request.LoginRequest;
import com.mealguide.mealguide_api.login.presentation.dto.request.LogoutRequest;
import com.mealguide.mealguide_api.login.presentation.dto.request.RefreshTokenRequest;
import com.mealguide.mealguide_api.login.presentation.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthApi {

    @SecurityRequirements
    @Operation(
            summary = "구글 로그인",
            description = "클라이언트가 전달한 구글 idToken과 deviceId를 검증한 뒤 서버 access token과 refresh token을 발급합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = AuthResponse.class, description = "로그인 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR),
                    @SwaggerApiFailedResponse(ErrorCode.GOOGLE_ID_TOKEN_INVALID),
                    @SwaggerApiFailedResponse(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED)
            }
    )
    ResponseEntity<ResponseBody<AuthResponse>> login(@Valid @RequestBody LoginRequest request);

    @SecurityRequirements
    @Operation(
            summary = "토큰 재발급",
            description = "refresh token을 검증하고 Redis 저장값과 비교한 뒤 access token과 refresh token을 재발급합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = AuthResponse.class, description = "재발급 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR),
                    @SwaggerApiFailedResponse(ErrorCode.JWT_INVALID),
                    @SwaggerApiFailedResponse(ErrorCode.JWT_EXPIRED),
                    @SwaggerApiFailedResponse(ErrorCode.REFRESH_TOKEN_INVALID)
            }
    )
    ResponseEntity<ResponseBody<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request);

    @Operation(
            summary = "로그아웃",
            description = "인증된 사용자와 요청 본문의 refresh token을 기준으로 Redis refresh token을 제거합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(description = "로그아웃 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.JWT_INVALID),
                    @SwaggerApiFailedResponse(ErrorCode.JWT_EXPIRED),
                    @SwaggerApiFailedResponse(ErrorCode.REFRESH_TOKEN_INVALID)
            }
    )
    ResponseEntity<ResponseBody<Void>> logout(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody LogoutRequest request
    );
}
