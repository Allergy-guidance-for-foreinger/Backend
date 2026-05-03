package com.mealguide.mealguide_api.review.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.review.domain.MenuReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuReviewLikeJpaRepository extends JpaRepository<MenuReviewLike, Long> {
}
