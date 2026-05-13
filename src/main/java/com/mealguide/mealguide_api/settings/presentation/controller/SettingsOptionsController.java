package com.mealguide.mealguide_api.settings.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.settings.application.service.SettingsService;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.CountryOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.SchoolOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.swagger.SettingsOptionsApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1/settings/options")
public class SettingsOptionsController implements SettingsOptionsApi {

    private final SettingsService settingsService;

    @GetMapping("/languages")
    public ResponseEntity<ResponseBody<LanguageOptionsResponse>> getLanguageOptions() {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(settingsService.getLanguageOptions()));
    }

    @GetMapping("/allergies")
    public ResponseEntity<ResponseBody<AllergyOptionsResponse>> getAllergyOptions(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(settingsService.getAllergyOptions(currentUserId)));
    }

    @GetMapping("/religions")
    public ResponseEntity<ResponseBody<ReligionOptionsResponse>> getReligionOptions(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(settingsService.getReligionOptions(currentUserId)));
    }

    @GetMapping("/countries")
    public ResponseEntity<ResponseBody<CountryOptionsResponse>> getCountryOptions(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(settingsService.getCountryOptions(currentUserId)));
    }

    @GetMapping("/schools")
    public ResponseEntity<ResponseBody<SchoolOptionsResponse>> getSchoolOptions(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(settingsService.getSchoolOptions(currentUserId)));
    }
}


