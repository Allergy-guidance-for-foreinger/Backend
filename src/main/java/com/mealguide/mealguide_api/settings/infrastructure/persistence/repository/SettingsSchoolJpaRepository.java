package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettingsSchoolJpaRepository extends JpaRepository<School, Long> {

    interface SchoolOptionProjection {
        Long getSchoolId();
        String getName();
    }

    @Query(value = """
            select s.id as schoolId,
                   coalesce(st.name, s.name) as name
            from school s
            left join school_translation st
              on st.school_id = s.id
             and st.lang_code = :langCode
            order by s.id asc
            """, nativeQuery = true)
    List<SchoolOptionProjection> findSchoolOptions(@Param("langCode") String langCode);
}
