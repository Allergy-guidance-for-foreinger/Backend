package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MealSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MealScheduleJpaRepository extends JpaRepository<MealSchedule, Long> {

    Optional<MealSchedule> findByCafeteriaIdAndMealDateAndMealType(Long cafeteriaId, LocalDate mealDate, String mealType);
}

