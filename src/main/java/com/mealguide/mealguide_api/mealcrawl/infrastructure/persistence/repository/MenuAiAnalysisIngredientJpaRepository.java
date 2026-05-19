package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisIngredient;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisIngredientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuAiAnalysisIngredientJpaRepository extends JpaRepository<MenuAiAnalysisIngredient, MenuAiAnalysisIngredientId> {
    @Modifying
    @Query("delete from MenuAiAnalysisIngredient ingredient where ingredient.menuAiAnalysisId = :menuAiAnalysisId")
    void deleteByMenuAiAnalysisId(@Param("menuAiAnalysisId") Long menuAiAnalysisId);
}

