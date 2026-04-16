package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface MenuAiAnalysisJpaRepository extends JpaRepository<MenuAiAnalysis, Long> {

    @Query("""
            select distinct analysis.menuId
            from MenuAiAnalysis analysis
            where analysis.menuId in :menuIds
            """)
    Set<Long> findAnalyzedMenuIds(@Param("menuIds") Set<Long> menuIds);
}

