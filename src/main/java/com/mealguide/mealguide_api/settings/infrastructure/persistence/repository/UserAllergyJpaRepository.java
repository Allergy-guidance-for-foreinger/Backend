package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.UserAllergy;
import com.mealguide.mealguide_api.settings.domain.UserAllergyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAllergyJpaRepository extends JpaRepository<UserAllergy, UserAllergyId> {
    @Query("""
            select ua.allergyCode
            from UserAllergy ua
            join Allergy a on a.code = ua.allergyCode
            where ua.userId = :userId
            order by a.displayOrder asc
            """)
    List<String> findAllergyCodesByUserIdOrderByDisplayOrder(@Param("userId") Long userId);

    @Modifying
    @Query("delete from UserAllergy ua where ua.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}

