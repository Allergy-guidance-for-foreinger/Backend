package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.util.List;

public record PythonIngredientTranslationResponse(
        List<PythonIngredientTranslationResultDto> results
) {
}
