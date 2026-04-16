package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PythonMenuAnalysisResultDto(
        Long menuId,
        String menuName,
        String status,
        String reason,
        String modelName,
        String modelVersion,
        LocalDateTime analyzedAt,
        List<PythonMenuIngredientResultDto> ingredients
) {
}



