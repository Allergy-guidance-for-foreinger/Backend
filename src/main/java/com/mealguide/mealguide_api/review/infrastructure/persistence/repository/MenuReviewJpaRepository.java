package com.mealguide.mealguide_api.review.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.review.domain.MenuReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuReviewJpaRepository extends JpaRepository<MenuReview, Long> {
}
