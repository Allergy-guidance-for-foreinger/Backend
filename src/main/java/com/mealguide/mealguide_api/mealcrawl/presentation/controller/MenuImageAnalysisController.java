package com.mealguide.mealguide_api.mealcrawl.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.mealcrawl.application.service.MenuImageAnalysisService;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuImageAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuImageAnalysisUsageResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.swagger.MenuImageAnalysisApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1")
public class MenuImageAnalysisController implements MenuImageAnalysisApi {

    private final MenuImageAnalysisService menuImageAnalysisService;

    @PostMapping(value = "/menus/analyze-image", consumes = {"multipart/form-data"})
    public ResponseEntity<ResponseBody<MenuImageAnalysisResponse>> analyzeImage(
            @CurrentUserId Long currentUserId,
            @RequestPart("image") MultipartFile image
    ) {
        MenuImageAnalysisResponse response = menuImageAnalysisService.analyze(currentUserId, image);
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(response));
    }

    @GetMapping("/menus/analyze-image/usage")
    public ResponseEntity<ResponseBody<MenuImageAnalysisUsageResponse>> getUsage(
            @CurrentUserId Long currentUserId
    ) {
        MenuImageAnalysisUsageResponse response = menuImageAnalysisService.getUsage(currentUserId);
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(response));
    }
}
