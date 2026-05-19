package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisAllergy;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisAllergyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuAiAnalysisAllergyJpaRepository extends JpaRepository<MenuAiAnalysisAllergy, MenuAiAnalysisAllergyId> {
    @Modifying
    @Query("delete from MenuAiAnalysisAllergy allergy where allergy.menuAiAnalysisId = :menuAiAnalysisId")
    void deleteByMenuAiAnalysisId(@Param("menuAiAnalysisId") Long menuAiAnalysisId);
}

