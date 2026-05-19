package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuAiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MenuAiAnalysisJpaRepository extends JpaRepository<MenuAiAnalysis, Long> {

    @Query("""
            select distinct analysis.menuId
            from MenuAiAnalysis analysis
            where analysis.menuId in :menuIds
              and analysis.status = com.mealguide.mealguide_api.mealcrawl.domain.MenuAiStatus.SUCCESS
            """)
    Set<Long> findAnalyzedMenuIds(@Param("menuIds") Set<Long> menuIds);

    @Query(value = """
            select ranked.menu_id
            from (
                select maa.menu_id,
                       maa.status,
                       maa.attempt_count,
                       row_number() over (
                           partition by maa.menu_id
                           order by coalesce(maa.analyzed_at, maa.created_at) desc, maa.id desc
                       ) as rn
                from menu_ai_analysis maa
            ) ranked
            where ranked.rn = 1
              and ranked.status = :failedStatus
              and ranked.attempt_count < :maxAttempt
            order by ranked.menu_id asc
            limit :limit
            """, nativeQuery = true)
    List<Long> findLatestFailedMenuIdsWithAttemptBelow(
            @Param("failedStatus") String failedStatus,
            @Param("maxAttempt") int maxAttempt,
            @Param("limit") int limit
    );

    @Query("""
            select analysis
            from MenuAiAnalysis analysis
            where analysis.menuId = :menuId
            order by coalesce(analysis.analyzedAt, analysis.createdAt) desc, analysis.id desc
            """)
    List<MenuAiAnalysis> findLatestByMenuId(@Param("menuId") Long menuId);

    default Optional<MenuAiAnalysis> findTopLatestByMenuId(Long menuId) {
        List<MenuAiAnalysis> rows = findLatestByMenuId(menuId);
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }
}

