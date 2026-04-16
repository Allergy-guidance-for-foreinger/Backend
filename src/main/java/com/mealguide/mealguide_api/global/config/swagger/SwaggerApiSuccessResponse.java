package com.mealguide.mealguide_api.global.config.swagger;

import org.springframework.http.HttpStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ??? ë„ˆ?Œì´?˜ì? API ?¸ì¶œ???•ìƒ?ìœ¼ë¡??„ë£Œ?˜ì—ˆ???Œì˜ ?‘ë‹µ HTTP ?íƒœ ì½”ë“œ?€ ?‘ë‹µ ë³¸ë¬¸???€??
 * ?¤í‚¤ë§ˆë? ëª…ì‹œ?????ˆìŠµ?ˆë‹¤.
 *
 * @see com.mealguide.mealguide_api.global.config.swagger.SwaggerApiResponses
 * @see HttpStatus
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerApiSuccessResponse {
    /**
     * ë°˜í™˜??HTTP ?íƒœ ì½”ë“œë¥?ì§€?•í•©?ˆë‹¤.
     */
    HttpStatus status() default HttpStatus.OK;

    /**
     * ?¨ì¼ ê°ì²´ë¡?ë°˜í™˜??DTO ?´ë˜???€?…ì„ ì§€?•í•©?ˆë‹¤.
     * <p><code>responsePage</code>?€ ?¨ê»˜ ?¬ìš©?????†ìŠµ?ˆë‹¤.</p>
     */
    Class<?> response() default Void.class;

    /**
     * ?˜ì´ì§€?¤ì´?˜ëœ ë¦¬ìŠ¤???•íƒœë¡?ë°˜í™˜??DTO ?´ë˜???€?…ì„ ì§€?•í•©?ˆë‹¤.
     * <p><code>response</code>?€ ?¨ê»˜ ?¬ìš©?????†ìŠµ?ˆë‹¤.</p>
     */
    Class<?> responsePage() default Void.class;

    /**
     * Swagger UI???œì‹œ???‘ë‹µ ?¤ëª…??ê¸°ì¬?©ë‹ˆ??
     */
    String description() default "";
}
