package com.mealguide.mealguide_api.mealcrawl.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.mealcrawl.application.service.WeeklyMealQueryService;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.WeeklyMealResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.swagger.WeeklyMealApi;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1/mealcrawl")
public class WeeklyMealController implements WeeklyMealApi {

    private final WeeklyMealQueryService weeklyMealQueryService;

    @GetMapping("/weekly-meals")
    public ResponseEntity<ResponseBody<WeeklyMealResponse>> getWeeklyMeals(
            @CurrentUserId Long currentUserId,
            @RequestParam Long cafeteriaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        WeeklyMealResponse response = weeklyMealQueryService.getWeeklyMeals(currentUserId, cafeteriaId, weekStartDate);
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(response));
    }
}
