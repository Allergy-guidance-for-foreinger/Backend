package com.mealguide.mealguide_api.settings.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.settings.domain.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LanguageJpaRepository extends JpaRepository<Language, String> {
    List<Language> findAllByOrderByCodeAsc();

    boolean existsByCode(String code);
}
