package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MealMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MealMenuJpaRepository extends JpaRepository<MealMenu, Long> {

    Optional<MealMenu> findByMealScheduleIdAndDisplayOrder(Long mealScheduleId, Integer displayOrder);
}

