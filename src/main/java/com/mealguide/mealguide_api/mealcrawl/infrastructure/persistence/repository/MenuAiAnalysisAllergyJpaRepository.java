package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisAllergy;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysisAllergyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuAiAnalysisAllergyJpaRepository extends JpaRepository<MenuAiAnalysisAllergy, MenuAiAnalysisAllergyId> {
}

