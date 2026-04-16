package com.mealguide.mealguide_api.settings.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.settings.application.service.SettingsService;
import com.mealguide.mealguide_api.settings.application.service.UserPreferenceService;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.swagger.SettingsOptionsApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1/settings/options")
public class SettingsOptionsController implements SettingsOptionsApi {

    private final SettingsService settingsService;
    private final UserPreferenceService userPreferenceService;

    @GetMapping("/languages")
    public ResponseEntity<ResponseBody<LanguageOptionsResponse>> getLanguageOptions() {
        List<LanguageOptionItemResponse> languages = settingsService.getLanguages().stream()
                .map(language -> new LanguageOptionItemResponse(language.code(), language.name(), language.englishName()))
                .toList();
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(new LanguageOptionsResponse(languages)));
    }

    @GetMapping("/allergies")
    public ResponseEntity<ResponseBody<AllergyOptionsResponse>> getAllergyOptions(@CurrentUserId Long currentUserId) {
        String languageCode = userPreferenceService.getLanguage(currentUserId);
        List<AllergyOptionItemResponse> allergies = settingsService.getAllergies(languageCode).stream()
                .map(allergy -> new AllergyOptionItemResponse(allergy.code(), allergy.name()))
                .toList();
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(new AllergyOptionsResponse(allergies)));
    }

    @GetMapping("/religions")
    public ResponseEntity<ResponseBody<ReligionOptionsResponse>> getReligionOptions(@CurrentUserId Long currentUserId) {
        String languageCode = userPreferenceService.getLanguage(currentUserId);
        List<ReligionOptionItemResponse> religions = settingsService.getReligions(languageCode).stream()
                .map(religion -> new ReligionOptionItemResponse(religion.code(), religion.name()))
                .toList();
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(new ReligionOptionsResponse(religions)));
    }
}


