package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuLikePort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuLikeToggleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuLikeService {

    private final MenuLikePort menuLikePort;

    @Transactional
    public MenuLikeToggleResponse toggleLike(Long userId, Long mealMenuId) {
        MenuLikeTarget target = menuLikePort.findTargetByMealMenuId(mealMenuId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BINDING_ERROR));

        boolean liked = menuLikePort.existsLike(userId, target.cafeteriaId(), target.menuId());
        boolean likedByMe;
        if (liked) {
            menuLikePort.deleteLike(userId, target.cafeteriaId(), target.menuId());
            likedByMe = false;
        } else {
            try {
                menuLikePort.saveLike(userId, target.cafeteriaId(), target.menuId());
                likedByMe = true;
            } catch (DataIntegrityViolationException ignored) {
                likedByMe = true;
            }
        }

        long likeCount = menuLikePort.countLikes(target.cafeteriaId(), target.menuId());
        return new MenuLikeToggleResponse(
                mealMenuId,
                target.cafeteriaId(),
                target.menuId(),
                likeCount,
                likedByMe
        );
    }
}
