package com.mealguide.mealguide_api.mealcrawl.presentation.validation;

import com.mealguide.mealguide_api.mealcrawl.domain.TranslationLanguageCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TranslationLanguageCodeValidator implements ConstraintValidator<ValidTranslationLanguageCode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return TranslationLanguageCode.isSupported(value);
    }
}
