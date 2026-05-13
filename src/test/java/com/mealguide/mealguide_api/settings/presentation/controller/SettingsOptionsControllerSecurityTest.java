package com.mealguide.mealguide_api.settings.presentation.controller;

import com.mealguide.mealguide_api.settings.application.service.SettingsService;
import com.mealguide.mealguide_api.global.auth.security.AuthenticatedUserPrincipal;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsOptionsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettingsService settingsService;

    @Test
    void unauthenticatedRequestIsBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/settings/options/allergies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void primaryAndAdditionalEndpointsAreNotProvided() throws Exception {
        mockMvc.perform(get("/api/v1/settings/options/allergies/primary")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/settings/options/allergies/additional")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isNotFound());
    }

    @Test
    void allergyOptionsEndpointReturnsExpectedResponseShape() throws Exception {
        when(settingsService.getAllergyOptions(1L)).thenReturn(
                new AllergyOptionsResponse(List.of(
                        new AllergyOptionItemResponse("EGG", "Egg"),
                        new AllergyOptionItemResponse("CELERY", "Celery")
                ))
        );

        mockMvc.perform(get("/api/v1/settings/options/allergies")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allergies[0].code").value("EGG"))
                .andExpect(jsonPath("$.data.allergies[0].name").value("Egg"))
                .andExpect(jsonPath("$.data.allergies[1].code").value("CELERY"))
                .andExpect(jsonPath("$.data.allergies[1].name").value("Celery"));
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.authenticated(1L, "test-device");
        return new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
    }
}
