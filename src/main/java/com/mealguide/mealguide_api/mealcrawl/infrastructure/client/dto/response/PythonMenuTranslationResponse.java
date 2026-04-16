package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.util.List;

public record PythonMenuTranslationResponse(
        List<PythonMenuTranslationResultDto> results
) {
}



