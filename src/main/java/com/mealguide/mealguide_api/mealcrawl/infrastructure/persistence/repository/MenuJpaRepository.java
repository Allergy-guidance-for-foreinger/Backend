package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.repository;

import com.mealguide.mealguide_api.mealcrawl.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MenuJpaRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findFirstByName(String name);

    List<Menu> findByIdIn(Collection<Long> ids);
}

