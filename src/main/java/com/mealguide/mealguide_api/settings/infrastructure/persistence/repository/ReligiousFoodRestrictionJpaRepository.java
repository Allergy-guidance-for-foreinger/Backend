package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.ReligiousFoodRestriction;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReligiousFoodRestrictionJpaRepository extends JpaRepository<ReligiousFoodRestriction, String> {

    @Query("""
            select new com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption(
                restriction.code,
                coalesce(translation.name, restriction.name)
            )
            from ReligiousFoodRestriction restriction
            left join ReligiousFoodRestrictionTranslation translation
                on translation.religiousFoodRestrictionCode = restriction.code
                and translation.langCode = :langCode
            order by restriction.code asc
            """)
    List<ReligiousRestrictionOption> findReligiousRestrictionOptions(@Param("langCode") String langCode);

    boolean existsByCode(String code);
}

