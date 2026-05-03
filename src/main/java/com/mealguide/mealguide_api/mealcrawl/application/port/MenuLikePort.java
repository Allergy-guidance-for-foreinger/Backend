package com.mealguide.mealguide_api.mealcrawl.application.port;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MenuLikePort {

    Optional<MenuLikeTarget> findTargetByMealMenuId(Long mealMenuId);

    boolean existsLike(Long userId, Long cafeteriaId, Long menuId);

    void saveLike(Long userId, Long cafeteriaId, Long menuId);

    void deleteLike(Long userId, Long cafeteriaId, Long menuId);

    long countLikes(Long cafeteriaId, Long menuId);

    Map<MenuLikeTarget, Long> countLikesByTargets(Set<MenuLikeTarget> targets);

    Set<MenuLikeTarget> findLikedTargetsByUser(Long userId, Set<MenuLikeTarget> targets);
}
