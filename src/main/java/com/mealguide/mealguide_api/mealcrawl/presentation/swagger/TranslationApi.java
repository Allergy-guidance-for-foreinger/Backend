package com.mealguide.mealguide_api.mealcrawl.presentation.swagger;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses;
import com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.request.TranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.TranslationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@SecurityRequirement(name = "Access Token")
public interface TranslationApi {

    @Operation(
            summary = "일반 텍스트 번역",
            description = "사용자 요청 텍스트를 Python 번역 서버에 전달하고 번역 결과를 즉시 반환합니다."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(response = TranslationResponse.class, description = "번역 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ErrorCode.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ErrorCode.BINDING_ERROR),
                    @SwaggerApiFailedResponse(ErrorCode.UNEXPECTED_SERVER_ERROR)
            }
    )
    ResponseEntity<ResponseBody<TranslationResponse>> translate(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody TranslationRequest request
    );
}
