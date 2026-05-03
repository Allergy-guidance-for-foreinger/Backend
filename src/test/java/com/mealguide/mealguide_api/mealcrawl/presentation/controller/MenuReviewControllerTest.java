package com.mealguide.mealguide_api.mealcrawl.presentation.controller;

import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.SuccessResponseBody;
import com.mealguide.mealguide_api.review.application.service.MenuReviewService;
import com.mealguide.mealguide_api.review.presentation.controller.MenuReviewController;
import com.mealguide.mealguide_api.review.presentation.dto.response.MenuReviewListResponse;
import com.mealguide.mealguide_api.review.presentation.dto.response.PageInfoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuReviewControllerTest {

    @Mock
    private MenuReviewService menuReviewService;

    @InjectMocks
    private MenuReviewController menuReviewController;

    @Test
    void getReviewsWrapsResponse() {
        MenuReviewListResponse serviceResponse = new MenuReviewListResponse(
                10L, 1L, 25L, 0L, List.of(), new PageInfoResponse(0, 20, 0, 0, false)
        );
        when(menuReviewService.getReviews(1L, 10L, 0, 20)).thenReturn(serviceResponse);

        ResponseBody<MenuReviewListResponse> body = menuReviewController.getReviews(1L, 10L, 0, 20).getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        assertThat(((SuccessResponseBody<MenuReviewListResponse>) body).getData()).isEqualTo(serviceResponse);
        verify(menuReviewService).getReviews(1L, 10L, 0, 20);
    }
}
