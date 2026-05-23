package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuImageAnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuImageAnalysisLogJpaRepository extends JpaRepository<MenuImageAnalysisLog, Long> {
}

