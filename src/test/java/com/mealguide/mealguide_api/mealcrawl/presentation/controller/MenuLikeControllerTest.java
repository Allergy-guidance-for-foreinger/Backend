package com.mealguide.mealguide_api.mealcrawl.presentation.controller;

import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.SuccessResponseBody;
import com.mealguide.mealguide_api.mealcrawl.application.service.MenuLikeService;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuLikeToggleResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuLikeControllerTest {

    @Mock
    private MenuLikeService menuLikeService;

    @InjectMocks
    private MenuLikeController menuLikeController;

    @Test
    void toggleLikeWrapsServiceResponse() {
        MenuLikeToggleResponse serviceResponse = new MenuLikeToggleResponse(10L, 1L, 25L, 13L, true);
        when(menuLikeService.toggleLike(1L, 10L)).thenReturn(serviceResponse);

        ResponseBody<MenuLikeToggleResponse> body = menuLikeController.toggleLike(1L, 10L).getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        assertThat(((SuccessResponseBody<MenuLikeToggleResponse>) body).getData()).isEqualTo(serviceResponse);
        verify(menuLikeService).toggleLike(1L, 10L);
    }
}
