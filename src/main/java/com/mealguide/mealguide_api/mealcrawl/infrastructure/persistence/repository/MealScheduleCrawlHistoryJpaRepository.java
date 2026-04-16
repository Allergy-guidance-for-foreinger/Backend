package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MealScheduleCrawlHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealScheduleCrawlHistoryJpaRepository extends JpaRepository<MealScheduleCrawlHistory, Long> {
}

