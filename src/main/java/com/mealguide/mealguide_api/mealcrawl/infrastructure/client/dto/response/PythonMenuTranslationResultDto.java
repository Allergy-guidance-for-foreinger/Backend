package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.util.List;

public record PythonMenuTranslationResultDto(
        Long menuId,
        String sourceName,
        List<PythonTranslatedMenuNameDto> translations
) {
}



