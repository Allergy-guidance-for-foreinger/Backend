package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.domain.MealMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealMenuJpaRepository extends JpaRepository<MealMenu, Long> {

    Optional<MealMenu> findByMealScheduleIdAndDisplayOrder(Long mealScheduleId, Integer displayOrder);

    @Query("""
            select new com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow(
                mealSchedule.mealDate,
                mealSchedule.mealType,
                mealMenu.displayOrder,
                mealMenu.cornerName,
                mealMenu.id,
                menu.name,
                menu.spicyLevel,
                menu.aiAnalysisStatus
            )
            from MealMenu mealMenu
            join com.mealguide.mealguide_api.mealcrawl.domain.MealSchedule mealSchedule
                on mealSchedule.id = mealMenu.mealScheduleId
            join com.mealguide.mealguide_api.mealcrawl.domain.Menu menu
                on menu.id = mealMenu.menuId
            where mealSchedule.cafeteriaId = :cafeteriaId
              and mealSchedule.mealDate between :weekStartDate and :weekEndDate
            order by mealSchedule.mealDate asc, mealSchedule.mealType asc, mealMenu.displayOrder asc
            """)
    List<WeeklyMealCacheRow> findWeeklyMealsForCache(
            @Param("cafeteriaId") Long cafeteriaId,
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("weekEndDate") LocalDate weekEndDate
    );
}

