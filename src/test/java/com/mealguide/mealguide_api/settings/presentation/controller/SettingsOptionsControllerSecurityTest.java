package com.mealguide.mealguide_api.settings.presentation.controller;

import com.mealguide.mealguide_api.settings.application.service.SettingsService;
import com.mealguide.mealguide_api.global.auth.security.AuthenticatedUserPrincipal;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.mock.mockito.MockBean;
@SpringBootTest
@AutoConfigureMockMvc
class SettingsOptionsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SettingsService settingsService;

    @Test
    void unauthenticatedRequestIsBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/settings/options/allergies/primary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oldAllergyOptionsEndpointIsNotProvided() throws Exception {
        mockMvc.perform(get("/api/v1/settings/options/allergies")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isNotFound());
    }

    @Test
    void primaryAndAdditionalEndpointsReturnExpectedResponseShape() throws Exception {
        when(settingsService.getPrimaryAllergyOptions(1L)).thenReturn(
                new AllergyOptionsResponse(List.of(new AllergyOptionItemResponse("EGG", "Egg")))
        );
        when(settingsService.getAdditionalAllergyOptions(1L)).thenReturn(
                new AllergyOptionsResponse(List.of(new AllergyOptionItemResponse("CELERY", "Celery")))
        );

        mockMvc.perform(get("/api/v1/settings/options/allergies/primary")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allergies[0].code").value("EGG"))
                .andExpect(jsonPath("$.data.allergies[0].name").value("Egg"));

        mockMvc.perform(get("/api/v1/settings/options/allergies/additional")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allergies[0].code").value("CELERY"))
                .andExpect(jsonPath("$.data.allergies[0].name").value("Celery"));
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.authenticated(1L, "test-device");
        return new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
    }
}
