package com.mealguide.mealguide_api.onboarding.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.onboarding.domain.School;
import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SchoolJpaRepository extends JpaRepository<School, Long> {

    @Query("""
            select new com.mealguide.mealguide_api.onboarding.domain.SchoolOption(
                school.id,
                coalesce(translation.name, school.name)
            )
            from School school
            left join SchoolTranslation translation
                on translation.schoolId = school.id
                and translation.langCode = :langCode
            order by school.id asc
            """)
    List<SchoolOption> findSchoolOptions(@Param("langCode") String langCode);
}

