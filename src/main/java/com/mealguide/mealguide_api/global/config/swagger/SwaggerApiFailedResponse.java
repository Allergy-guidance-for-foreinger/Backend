package com.mealguide.mealguide_api.global.config.swagger;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ??? ë„ˆ?Œì´?˜ì? API ?¸ì¶œ???¤íŒ¨?ˆì„ ?Œì˜ ?‘ë‹µ HTTP ?íƒœ ì½”ë“œ?€ ?‘ë‹µ ë³¸ë¬¸???€??
 * ?¤í‚¤ë§ˆë? ëª…ì‹œ?????ˆìŠµ?ˆë‹¤.
 *
 * @see com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses
 * @see com.mealguide.mealguide_api.global.base.exception.ErrorCode
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerApiFailedResponse {
    /**
     * {@link ErrorCode}???•ì˜???ˆì™¸ ?€?…ì„ ì§€?•í•©?ˆë‹¤.
     */
    ErrorCode value();

    /**
     * Swagger UI???œì‹œ???¤íŒ¨ ?‘ë‹µ ?¤ëª…??ê¸°ì¬?©ë‹ˆ??
     * <p>ì§€?•í•˜ì§€ ?Šìœ¼ë©?{@link ErrorCode}??ê¸°ë³¸ ë©”ì‹œì§€ê°€ ?¬ìš©?©ë‹ˆ??</p>
     */
    String description() default "";
}

