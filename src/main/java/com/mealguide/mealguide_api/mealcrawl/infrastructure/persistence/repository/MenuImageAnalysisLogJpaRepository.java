package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuImageAnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface MenuImageAnalysisLogJpaRepository extends JpaRepository<MenuImageAnalysisLog, Long> {
    long countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );
}
