package com.mealguide.mealguide_api.mealcrawl.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.mealcrawl.application.service.MenuDetailQueryService;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuDetailResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.swagger.MenuDetailApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1/mealcrawl")
public class MenuDetailController implements MenuDetailApi {

    private final MenuDetailQueryService menuDetailQueryService;

    @GetMapping("/menus/{mealMenuId}")
    public ResponseEntity<ResponseBody<MenuDetailResponse>> getMenuDetail(
            @CurrentUserId Long currentUserId,
            @PathVariable Long mealMenuId
    ) {
        MenuDetailResponse response = menuDetailQueryService.getMenuDetail(currentUserId, mealMenuId);
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(response));
    }
}
