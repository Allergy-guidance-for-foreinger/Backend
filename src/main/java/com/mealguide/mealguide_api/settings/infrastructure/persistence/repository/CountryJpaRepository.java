package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CountryJpaRepository extends JpaRepository<Country, String> {
    List<Country> findAllByOrderByNameAsc();

    boolean existsByCode(String code);
}
