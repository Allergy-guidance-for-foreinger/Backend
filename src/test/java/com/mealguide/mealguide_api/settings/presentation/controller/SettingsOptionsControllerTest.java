package com.mealguide.mealguide_api.settings.presentation.controller;

import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.SuccessResponseBody;
import com.mealguide.mealguide_api.settings.application.service.SettingsService;
import com.mealguide.mealguide_api.settings.application.service.UserPreferenceService;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsOptionsControllerTest {

    @Mock
    private SettingsService settingsService;

    @Mock
    private UserPreferenceService userPreferenceService;

    @InjectMocks
    private SettingsOptionsController settingsOptionsController;

    @Test
    void getLanguageOptionsReturnsFullLanguageList() {
        when(settingsService.getLanguages()).thenReturn(List.of(
                new LanguageOption("ko", "Korean", "Korean"),
                new LanguageOption("en", "English", "English")
        ));

        ResponseBody<LanguageOptionsResponse> body = settingsOptionsController.getLanguageOptions().getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        LanguageOptionsResponse response = ((SuccessResponseBody<LanguageOptionsResponse>) body).getData();
        assertThat(response.languages())
                .extracting(item -> item.code())
                .containsExactly("ko", "en");
        assertThat(response.languages())
                .extracting(item -> item.name())
                .containsExactly("Korean", "English");
    }

    @Test
    void getAllergyOptionsUsesUserLanguage() {
        when(userPreferenceService.getLanguage(1L)).thenReturn("en");
        when(settingsService.getAllergies("en")).thenReturn(List.of(
                new AllergyOption("EGG", "Egg", 1),
                new AllergyOption("MILK", "Milk", 2)
        ));

        ResponseBody<AllergyOptionsResponse> body = settingsOptionsController.getAllergyOptions(1L).getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        AllergyOptionsResponse response = ((SuccessResponseBody<AllergyOptionsResponse>) body).getData();
        assertThat(response.allergies())
                .extracting(item -> item.code())
                .containsExactly("EGG", "MILK");
        assertThat(response.allergies())
                .extracting(item -> item.name())
                .containsExactly("Egg", "Milk");
        verify(settingsService).getAllergies("en");
    }

    @Test
    void getReligionOptionsUsesUserLanguage() {
        when(userPreferenceService.getLanguage(1L)).thenReturn("en");
        when(settingsService.getReligions("en")).thenReturn(List.of(
                new ReligiousRestrictionOption("HALAL", "Halal"),
                new ReligiousRestrictionOption("HINDU", "Hindu")
        ));

        ResponseBody<ReligionOptionsResponse> body = settingsOptionsController.getReligionOptions(1L).getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        ReligionOptionsResponse response = ((SuccessResponseBody<ReligionOptionsResponse>) body).getData();
        assertThat(response.religions())
                .extracting(item -> item.code())
                .containsExactly("HALAL", "HINDU");
        assertThat(response.religions())
                .extracting(item -> item.name())
                .containsExactly("Halal", "Hindu");
        verify(settingsService).getReligions("en");
    }
}
