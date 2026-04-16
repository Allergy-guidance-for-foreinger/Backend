package com.mealguide.mealguide_api.global.config.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * ??? ë„ˆ?Œì´?˜ì? API ?”ë“œ ?¬ì¸?¸ì—?œì˜ ?±ê³µ ë°??¤ë¥˜ ?‘ë‹µ???€???¤ëª…???•ì˜?©ë‹ˆ??
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

