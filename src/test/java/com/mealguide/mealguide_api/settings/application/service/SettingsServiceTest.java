package com.mealguide.mealguide_api.settings.application.service;

import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.CountryOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import com.mealguide.mealguide_api.settings.domain.SchoolOption;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.CountryOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.SchoolOptionsResponse;
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
class SettingsServiceTest {

    @Mock
    private SettingsMasterQueryPort settingsMasterQueryPort;

    @Mock
    private UserPreferenceService userPreferenceService;

    @InjectMocks
    private SettingsService settingsService;

    @Test
    void getLanguageOptionsReturnsMappedResponse() {
        when(settingsMasterQueryPort.findLanguages()).thenReturn(List.of(
                new LanguageOption("en", "English", "English")
        ));

        LanguageOptionsResponse response = settingsService.getLanguageOptions();

        assertThat(response.languages()).extracting(item -> item.code()).containsExactly("en");
        assertThat(response.languages()).extracting(item -> item.name()).containsExactly("English");
        assertThat(response.languages()).extracting(item -> item.englishName()).containsExactly("English");
    }

    @Test
    void getAllergyOptionsUsesUserLanguageAndReturnsMappedResponse() {
        when(userPreferenceService.getLanguage(1L)).thenReturn("en");
        when(settingsMasterQueryPort.findAllergyOptions("en")).thenReturn(List.of(
                new AllergyOption("EGG", "Egg", 1),
                new AllergyOption("CELERY", "Celery", 2)
        ));

        AllergyOptionsResponse response = settingsService.getAllergyOptions(1L);

        assertThat(response.allergies()).extracting(item -> item.code()).containsExactly("EGG", "CELERY");
        assertThat(response.allergies()).extracting(item -> item.name()).containsExactly("Egg", "Celery");
        verify(userPreferenceService).getLanguage(1L);
        verify(settingsMasterQueryPort).findAllergyOptions("en");
    }

    @Test
    void getReligionOptionsUsesUserLanguageAndReturnsMappedResponse() {
        when(userPreferenceService.getLanguage(1L)).thenReturn("en");
        when(settingsMasterQueryPort.findReligiousRestrictions("en")).thenReturn(List.of(
                new ReligiousRestrictionOption("HALAL", "Halal")
        ));

        ReligionOptionsResponse response = settingsService.getReligionOptions(1L);

        assertThat(response.religions()).extracting(item -> item.code()).containsExactly("HALAL");
        assertThat(response.religions()).extracting(item -> item.name()).containsExactly("Halal");
        verify(userPreferenceService).getLanguage(1L);
        verify(settingsMasterQueryPort).findReligiousRestrictions("en");
    }

    @Test
    void getCountryOptionsUsesUserLanguageAndReturnsLocalizedResponse() {
        when(userPreferenceService.getLanguage(1L)).thenReturn("ko");
        when(settingsMasterQueryPort.findCountries()).thenReturn(List.of(
                new CountryOption("KR", "Korea"),
                new CountryOption("US", "United States")
        ));

        CountryOptionsResponse response = settingsService.getCountryOptions(1L);

        assertThat(response.countries()).extracting(item -> item.code()).containsExactly("KR", "US");
        assertThat(response.countries()).extracting(item -> item.name()).containsExactly("대한민국", "미국");
        verify(userPreferenceService).getLanguage(1L);
    }

    @Test
    void getCountryOptionsFallsBackToStoredNameWhenCodeIsInvalid() {
        when(userPreferenceService.getLanguage(1L)).thenReturn("en");
        when(settingsMasterQueryPort.findCountries()).thenReturn(List.of(
                new CountryOption("XXX", "Unknown Country")
        ));

        CountryOptionsResponse response = settingsService.getCountryOptions(1L);

        assertThat(response.countries()).extracting(item -> item.code()).containsExactly("XXX");
        assertThat(response.countries()).extracting(item -> item.name()).containsExactly("Unknown Country");
        verify(userPreferenceService).getLanguage(1L);
    }

    @Test
    void getSchoolOptionsUsesUserLanguageAndReturnsMappedResponse() {
        when(userPreferenceService.getLanguage(1L)).thenReturn("en");
        when(settingsMasterQueryPort.findSchools("en")).thenReturn(List.of(
                new SchoolOption(1L, "Kumoh National Institute of Technology"),
                new SchoolOption(2L, "Base School Name")
        ));

        SchoolOptionsResponse response = settingsService.getSchoolOptions(1L);

        assertThat(response.schools()).extracting(item -> item.schoolId()).containsExactly(1L, 2L);
        assertThat(response.schools()).extracting(item -> item.name())
                .containsExactly("Kumoh National Institute of Technology", "Base School Name");
        verify(userPreferenceService).getLanguage(1L);
        verify(settingsMasterQueryPort).findSchools("en");
    }
}
