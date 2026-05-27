package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

public record PythonIngredientTranslationResultDto(
        String ingredientCode,
        String translatedText
) {
}
