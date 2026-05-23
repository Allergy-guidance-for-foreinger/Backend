package com.mealguide.mealguide_api.mealcrawl.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TranslationLanguageCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTranslationLanguageCode {
    String message() default "유효하지 않은 언어 코드입니다. (지원: ko, en)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
