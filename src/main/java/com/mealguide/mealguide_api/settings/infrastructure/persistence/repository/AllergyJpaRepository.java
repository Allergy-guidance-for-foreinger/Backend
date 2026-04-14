package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.Allergy;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface AllergyJpaRepository extends JpaRepository<Allergy, String> {

    @Query("""
            select new com.mealguide.mealguide_api.settings.domain.AllergyOption(
                allergy.code,
                coalesce(translation.name, allergy.name),
                allergy.displayOrder
            )
            from Allergy allergy
            left join AllergyTranslation translation
                on translation.allergyCode = allergy.code
                and translation.langCode = :langCode
            order by allergy.displayOrder asc
            """)
    List<AllergyOption> findAllergyOptions(@Param("langCode") String langCode);

    long countByCodeIn(Set<String> codes);
}
