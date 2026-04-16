package com.mealguide.mealguide_api.mealcrawl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "menu_ai_analysis_ingredient")
@IdClass(MenuAiAnalysisIngredientId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuAiAnalysisIngredient {

    @Id
    @Column(name = "menu_ai_analysis_id", nullable = false)
    private Long menuAiAnalysisId;

    @Id
    @Column(name = "ingredient_code", nullable = false, length = 50)
    private String ingredientCode;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MenuAiAnalysisIngredient create(Long menuAiAnalysisId, String ingredientCode, BigDecimal confidence) {
        MenuAiAnalysisIngredient ingredient = new MenuAiAnalysisIngredient();
        ingredient.menuAiAnalysisId = menuAiAnalysisId;
        ingredient.ingredientCode = ingredientCode;
        ingredient.confidence = confidence;
        ingredient.createdAt = LocalDateTime.now();
        return ingredient;
    }
}

