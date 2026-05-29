package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request;

import java.util.List;

public record PythonMenuDescriptionRequest(
        String langCode,
        List<PythonMenuDescriptionTargetDto> menus
) {
}
