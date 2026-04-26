package com.mealguide.mealguide_api.mealcrawl.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.mealcrawl.application.service.CafeteriaQueryService;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.CafeteriaListResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.swagger.CafeteriaApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1/mealcrawl")
public class CafeteriaController implements CafeteriaApi {

    private final CafeteriaQueryService cafeteriaQueryService;

    @GetMapping("/cafeterias")
    public ResponseEntity<ResponseBody<CafeteriaListResponse>> getCafeterias(
            @CurrentUserId Long currentUserId
    ) {
        CafeteriaListResponse response = cafeteriaQueryService.getCafeteriasForCurrentUser(currentUserId);
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(response));
    }
}
