package com.mealguide.mealguide_api.settings.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.settings.application.service.UserPreferenceService;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateAllergiesRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateCountryRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateLanguageRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateReligionRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.request.UpdateSchoolRequest;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.CountryUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.SchoolSettingResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.SchoolUpdateResponse;
import com.mealguide.mealguide_api.settings.presentation.swagger.SettingsApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/country")
    public ResponseEntity<ResponseBody<CountryUpdateResponse>> getCountry(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                new CountryUpdateResponse(userPreferenceService.getCountry(currentUserId))
        ));
    }

    @GetMapping("/school")
    public ResponseEntity<ResponseBody<SchoolSettingResponse>> getSchool(@CurrentUserId Long currentUserId) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                new SchoolSettingResponse(userPreferenceService.getSchool(currentUserId))
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
        List<String> religiousCodes = userPreferenceService.updateReligion(currentUserId, request.religiousCodes());
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(new ReligionUpdateResponse(religiousCodes)));
    }

    @PatchMapping("/country")
    public ResponseEntity<ResponseBody<CountryUpdateResponse>> updateCountry(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody UpdateCountryRequest request
    ) {
        String countryCode = userPreferenceService.updateCountry(currentUserId, request.countryCode());
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(new CountryUpdateResponse(countryCode)));
    }

    @PatchMapping("/school")
    public ResponseEntity<ResponseBody<SchoolUpdateResponse>> updateSchool(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody UpdateSchoolRequest request
    ) {
        Long schoolId = userPreferenceService.updateSchool(currentUserId, request.schoolId());
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(new SchoolUpdateResponse(schoolId)));
    }
}

