package com.mealguide.mealguide_api.onboarding.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.onboarding.application.service.OnboardingService;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.onboarding.presentation.dto.request.CompleteOnboardingRequest;
import com.mealguide.mealguide_api.onboarding.presentation.dto.response.CompleteOnboardingResponse;
import com.mealguide.mealguide_api.onboarding.presentation.dto.response.SchoolListResponse;
import com.mealguide.mealguide_api.onboarding.presentation.swagger.OnboardingApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ResponseBody<CompleteOnboardingResponse>> completeOnboarding(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody CompleteOnboardingRequest request
    ) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                CompleteOnboardingResponse.from(
                        onboardingService.completeOnboarding(
                                currentUserId,
                                request.languageCode(),
                                request.schoolId(),
                                request.allergyCodes(),
                                request.religiousCode()
                        )
                )
        ));
    }
}

