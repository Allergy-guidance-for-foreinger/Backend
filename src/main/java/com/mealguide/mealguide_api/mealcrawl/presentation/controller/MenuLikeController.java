package com.mealguide.mealguide_api.mealcrawl.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.mealcrawl.application.service.MenuLikeService;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuLikeToggleResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.swagger.MenuLikeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1")
public class MenuLikeController implements MenuLikeApi {

    private final MenuLikeService menuLikeService;

    @PostMapping("/meal-menus/{mealMenuId}/like")
    public ResponseEntity<ResponseBody<MenuLikeToggleResponse>> toggleLike(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId
    ) {
        MenuLikeToggleResponse response = menuLikeService.toggleLike(currentUserId, mealMenuId);
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(response));
    }
}
