package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuLikePort;
import com.mealguide.mealguide_api.mealcrawl.domain.MenuLikeTarget;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuLikeToggleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuLikeServiceTest {

    @Test
    void toggleCreatesLikeWhenNotExists() {
        MenuLikePort menuLikePort = mock(MenuLikePort.class);
        MenuLikeService service = new MenuLikeService(menuLikePort);
        when(menuLikePort.findTargetByMealMenuId(10L)).thenReturn(Optional.of(new MenuLikeTarget(1L, 25L)));
        when(menuLikePort.existsLike(1L, 1L, 25L)).thenReturn(false, true);
        when(menuLikePort.countLikes(1L, 25L)).thenReturn(13L);

        MenuLikeToggleResponse response = service.toggleLike(1L, 10L);

        verify(menuLikePort).saveLike(1L, 1L, 25L);
        assertThat(response.likedByMe()).isTrue();
        assertThat(response.likeCount()).isEqualTo(13L);
    }

    @Test
    void toggleDeletesLikeWhenExists() {
        MenuLikePort menuLikePort = mock(MenuLikePort.class);
        MenuLikeService service = new MenuLikeService(menuLikePort);
        when(menuLikePort.findTargetByMealMenuId(10L)).thenReturn(Optional.of(new MenuLikeTarget(1L, 25L)));
        when(menuLikePort.existsLike(1L, 1L, 25L)).thenReturn(true, false);
        when(menuLikePort.countLikes(1L, 25L)).thenReturn(12L);

        MenuLikeToggleResponse response = service.toggleLike(1L, 10L);

        verify(menuLikePort).deleteLike(1L, 1L, 25L);
        assertThat(response.likedByMe()).isFalse();
        assertThat(response.likeCount()).isEqualTo(12L);
    }

    @Test
    void toggleHandlesUniqueConflictAsAlreadyLiked() {
        MenuLikePort menuLikePort = mock(MenuLikePort.class);
        MenuLikeService service = new MenuLikeService(menuLikePort);
        when(menuLikePort.findTargetByMealMenuId(10L)).thenReturn(Optional.of(new MenuLikeTarget(1L, 25L)));
        when(menuLikePort.existsLike(1L, 1L, 25L)).thenReturn(false, true);
        doThrow(new DataIntegrityViolationException("duplicate")).when(menuLikePort).saveLike(1L, 1L, 25L);
        when(menuLikePort.countLikes(1L, 25L)).thenReturn(13L);

        MenuLikeToggleResponse response = service.toggleLike(1L, 10L);

        assertThat(response.likedByMe()).isTrue();
        assertThat(response.likeCount()).isEqualTo(13L);
    }

    @Test
    void toggleFailsWhenMealMenuMissing() {
        MenuLikePort menuLikePort = mock(MenuLikePort.class);
        MenuLikeService service = new MenuLikeService(menuLikePort);
        when(menuLikePort.findTargetByMealMenuId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggleLike(1L, 99L))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BINDING_ERROR);
    }
}
