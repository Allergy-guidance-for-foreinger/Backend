package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request;

import java.util.List;

public record PythonMenuTranslationRequest(
        List<PythonMenuTranslationTargetDto> menus,
        List<String> targetLanguages
) {
}



