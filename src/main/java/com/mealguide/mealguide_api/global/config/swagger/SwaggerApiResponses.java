package com.mealguide.mealguide_api.global.config.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 어노테이션된 API 메서드 사양에서 성공 및 오류 응답에 대한 설명을 정의합니다.
 *
 * @see com.mealguide.mealguide_api.global.config.swagger.SwaggerApiSuccessResponse
 * @see com.mealguide.mealguide_api.global.config.swagger.SwaggerApiFailedResponse
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerApiResponses {
    SwaggerApiSuccessResponse success() default @SwaggerApiSuccessResponse;

    SwaggerApiFailedResponse[] errors() default { };
}
