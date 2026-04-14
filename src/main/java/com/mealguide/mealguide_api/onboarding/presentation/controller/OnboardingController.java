package com.mealguide.mealguide_api.onboarding.presentation.controller;

import com.mealguide.mealguide_api.onboarding.application.service.OnboardingService;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.onboarding.presentation.dto.response.SchoolListResponse;
import com.mealguide.mealguide_api.onboarding.presentation.swagger.OnboardingApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/onboarding")
public class OnboardingController implements OnboardingApi {

    private final OnboardingService onboardingService;

    @GetMapping("/schools")
    public ResponseEntity<ResponseBody<SchoolListResponse>> getSchools(@RequestParam(required = false) String lang) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                SchoolListResponse.from(onboardingService.getSchools(lang))
        ));
    }
}
