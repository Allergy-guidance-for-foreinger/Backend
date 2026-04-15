package com.mealguide.mealguide_api.settings.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.settings.application.service.UserPreferenceService;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateAllergiesRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateLanguageRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateReligionRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.swagger.SettingsApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1/settings")
public class UserSettingsController implements SettingsApi {

    private final UserPreferenceService userPreferenceService;

    @GetMapping("/language")
    public ResponseEntity<ResponseBody<LanguageUpdateResponse>> getLanguage(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                new LanguageUpdateResponse(userPreferenceService.getLanguage(currentUserId))
        ));
    }

    @GetMapping("/allergies")
    public ResponseEntity<ResponseBody<AllergyUpdateResponse>> getAllergies(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                new AllergyUpdateResponse(userPreferenceService.getAllergies(currentUserId))
        ));
    }

    @GetMapping("/religion")
    public ResponseEntity<ResponseBody<ReligionUpdateResponse>> getReligion(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                new ReligionUpdateResponse(userPreferenceService.getReligion(currentUserId))
        ));
    }

    @PatchMapping("/language")
    public ResponseEntity<ResponseBody<LanguageUpdateResponse>> updateLanguage(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody UpdateLanguageRequest request
    ) {
        String languageCode = userPreferenceService.updateLanguage(currentUserId, request.languageCode());
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(new LanguageUpdateResponse(languageCode)));
    }

    @PutMapping("/allergies")
    public ResponseEntity<ResponseBody<AllergyUpdateResponse>> updateAllergies(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody UpdateAllergiesRequest request
    ) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                new AllergyUpdateResponse(userPreferenceService.replaceAllergies(currentUserId, request.allergyCodes()))
        ));
    }

    @PatchMapping("/religion")
    public ResponseEntity<ResponseBody<ReligionUpdateResponse>> updateReligion(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody UpdateReligionRequest request
    ) {
        String religiousCode = userPreferenceService.updateReligion(currentUserId, request.religiousCode());
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(new ReligionUpdateResponse(religiousCode)));
    }
}
