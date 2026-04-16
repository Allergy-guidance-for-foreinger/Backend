package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MenuTranslationJpaRepository extends JpaRepository<MenuTranslation, Long> {

    List<MenuTranslation> findByMenuIdInAndLangCodeIn(Collection<Long> menuIds, Collection<String> langCodes);
}

