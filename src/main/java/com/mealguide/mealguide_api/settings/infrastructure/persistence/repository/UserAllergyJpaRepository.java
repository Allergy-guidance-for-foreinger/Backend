package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.UserAllergy;
import com.mealguide.mealguide_api.settings.domain.UserAllergyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAllergyJpaRepository extends JpaRepository<UserAllergy, UserAllergyId> {
    @Query(value = """
            select user_allergy.allergy_code
            from user_allergy
            join allergy on allergy.code = user_allergy.allergy_code
            where user_allergy.user_id = :userId
            order by allergy.display_order asc
            """, nativeQuery = true)
    List<String> findAllergyCodesByUserIdOrderByDisplayOrder(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
