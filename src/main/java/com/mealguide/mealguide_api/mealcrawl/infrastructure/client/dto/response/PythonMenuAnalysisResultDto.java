package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record PythonMenuAnalysisResultDto(
        Long menuId,
        String menuName,
        PythonMenuAnalysisStatus status,
        @JsonAlias({"spicy_level"})
        Long spicyLevel,
        String reason,
        String modelName,
        String modelVersion,
        List<PythonMenuIngredientResultDto> ingredients,
        List<PythonMenuAllergyResultDto> allergies
) {
    public PythonMenuAnalysisResultDto(
            Long menuId,
            String menuName,
            PythonMenuAnalysisStatus status,
            String reason,
            String modelName,
            String modelVersion,
            List<PythonMenuIngredientResultDto> ingredients
    ) {
        this(menuId, menuName, status, null, reason, modelName, modelVersion, ingredients, List.of());
    }
}



