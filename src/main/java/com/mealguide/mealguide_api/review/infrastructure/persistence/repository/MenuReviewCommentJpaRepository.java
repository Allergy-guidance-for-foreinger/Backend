package com.mealguide.mealguide_api.review.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.review.domain.MenuReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuReviewCommentJpaRepository extends JpaRepository<MenuReviewComment, Long> {
}
