package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.School;
import com.mealguide.mealguide_api.settings.domain.SchoolOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettingsSchoolJpaRepository extends JpaRepository<School, Long> {

    interface SchoolOptionProjection {
        Long getSchoolId();
        String getName();
    }

    @Query("""
            select new com.mealguide.mealguide_api.settings.domain.SchoolOption(
                s.id,
                coalesce(st.name, s.name)
            )
            from School s
            left join SchoolTranslation st
                on st.schoolId = s.id
                and st.langCode = :langCode
            order by s.id asc
            """)
    List<SchoolOption> findSchoolOptions(@Param("langCode") String langCode);
}
