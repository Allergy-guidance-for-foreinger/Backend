package com.mealguide.mealguide_api.settings.presentation.controller;

import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.SuccessResponseBody;
import com.mealguide.mealguide_api.settings.application.service.SettingsService;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.CountryOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.CountryOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.SchoolOptionItemResponse;
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
class SettingsOptionsControllerTest {

    @Mock
    private SettingsService settingsService;

    @InjectMocks
    private SettingsOptionsController settingsOptionsController;

    @Test
    void getLanguageOptionsWrapsServiceResponse() {
        LanguageOptionsResponse serviceResponse = new LanguageOptionsResponse(
                List.of(new LanguageOptionItemResponse("en", "English", "English"))
        );
        when(settingsService.getLanguageOptions()).thenReturn(serviceResponse);

        ResponseBody<LanguageOptionsResponse> body = settingsOptionsController.getLanguageOptions().getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        assertThat(((SuccessResponseBody<LanguageOptionsResponse>) body).getData()).isEqualTo(serviceResponse);
        verify(settingsService).getLanguageOptions();
    }

    @Test
    void getAllergyOptionsWrapsServiceResponse() {
        AllergyOptionsResponse serviceResponse = new AllergyOptionsResponse(
                List.of(new AllergyOptionItemResponse("EGG", "Egg"))
        );
        when(settingsService.getAllergyOptions(1L)).thenReturn(serviceResponse);

        ResponseBody<AllergyOptionsResponse> body = settingsOptionsController.getAllergyOptions(1L).getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        assertThat(((SuccessResponseBody<AllergyOptionsResponse>) body).getData()).isEqualTo(serviceResponse);
        verify(settingsService).getAllergyOptions(1L);
    }

    @Test
    void getReligionOptionsWrapsServiceResponse() {
        ReligionOptionsResponse serviceResponse = new ReligionOptionsResponse(
                List.of(new ReligionOptionItemResponse("HALAL", "Halal"))
        );
        when(settingsService.getReligionOptions(1L)).thenReturn(serviceResponse);

        ResponseBody<ReligionOptionsResponse> body = settingsOptionsController.getReligionOptions(1L).getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        assertThat(((SuccessResponseBody<ReligionOptionsResponse>) body).getData()).isEqualTo(serviceResponse);
        verify(settingsService).getReligionOptions(1L);
    }

    @Test
    void getCountryOptionsWrapsServiceResponse() {
        CountryOptionsResponse serviceResponse = new CountryOptionsResponse(
                List.of(new CountryOptionItemResponse("KR", "Korea"))
        );
        when(settingsService.getCountryOptions(1L)).thenReturn(serviceResponse);

        ResponseBody<CountryOptionsResponse> body = settingsOptionsController.getCountryOptions(1L).getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        assertThat(((SuccessResponseBody<CountryOptionsResponse>) body).getData()).isEqualTo(serviceResponse);
        verify(settingsService).getCountryOptions(1L);
    }

    @Test
    void getSchoolOptionsWrapsServiceResponse() {
        SchoolOptionsResponse serviceResponse = new SchoolOptionsResponse(
                List.of(new SchoolOptionItemResponse(1L, "Kumoh National Institute of Technology"))
        );
        when(settingsService.getSchoolOptions(1L)).thenReturn(serviceResponse);

        ResponseBody<SchoolOptionsResponse> body = settingsOptionsController.getSchoolOptions(1L).getBody();

        assertThat(body).isInstanceOf(SuccessResponseBody.class);
        assertThat(((SuccessResponseBody<SchoolOptionsResponse>) body).getData()).isEqualTo(serviceResponse);
        verify(settingsService).getSchoolOptions(1L);
    }
}
