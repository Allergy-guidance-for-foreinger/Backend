package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisIngredient;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisIngredientId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuAiAnalysisIngredientJpaRepository extends JpaRepository<MenuAiAnalysisIngredient, MenuAiAnalysisIngredientId> {
}

