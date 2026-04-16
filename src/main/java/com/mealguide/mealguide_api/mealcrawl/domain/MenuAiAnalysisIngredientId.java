package com.mealguide.mealguide_api.mealcrawl.domain;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class MenuAiAnalysisIngredientId implements Serializable {
    private Long menuAiAnalysisId;
    private String ingredientCode;
}

