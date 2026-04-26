package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.application.dto.CafeteriaRow;
import com.mealguide.mealguide_api.mealcrawl.domain.Cafeteria;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CafeteriaJpaRepository extends JpaRepository<Cafeteria, Long> {

    boolean existsByIdAndSchoolId(Long id, Long schoolId);

    @Query("""
            select new com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource(
                school.id,
                cafeteria.id,
                school.name,
                cafeteria.name,
                school.sourceUrl
            )
            from Cafeteria cafeteria
            join com.mealguide.mealguide_api.onboarding.domain.School school
                on school.id = cafeteria.schoolId
            where school.sourceUrl is not null
              and trim(school.sourceUrl) <> ''
            order by cafeteria.id asc
            """)
    List<CrawlTargetSource> findAllCrawlTargets();

    @Query("""
            select new com.mealguide.mealguide_api.mealcrawl.application.dto.CafeteriaRow(
                cafeteria.id,
                cafeteria.name
            )
            from Cafeteria cafeteria
            where cafeteria.schoolId = :schoolId
            order by cafeteria.id asc
            """)
    List<CafeteriaRow> findCafeteriaRowsBySchoolId(@Param("schoolId") Long schoolId);
}

