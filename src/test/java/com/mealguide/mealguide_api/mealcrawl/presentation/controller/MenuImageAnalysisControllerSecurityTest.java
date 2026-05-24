package com.mealguide.mealguide_api.mealcrawl.presentation.controller;

import com.mealguide.mealguide_api.global.auth.security.AuthenticatedUserPrincipal;
import com.mealguide.mealguide_api.login.domain.UserRole;
import com.mealguide.mealguide_api.mealcrawl.application.service.MenuImageAnalysisService;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.MenuImageAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "mealguide.jwt.access-secret=test-access-secret-test-access-secret",
        "mealguide.jwt.refresh-secret=test-refresh-secret-test-refresh-secret",
        "mealguide.jwt.access-token-expiration-seconds=3600",
        "mealguide.jwt.refresh-token-expiration-seconds=7200",
        "mealguide.google.client-id=test-client-id",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
class MenuImageAnalysisControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuImageAnalysisService menuImageAnalysisService;

    @Test
    void unauthenticatedRequestIsBlocked() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/api/v1/menus/analyze-image").file(image))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedMultipartRequestIsAllowed() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1});
        when(menuImageAnalysisService.analyze(eq(1L), any())).thenReturn(
                new MenuImageAnalysisResponse(
                        102L,
                        MenuImageAnalysisResponse.MenuImageAnalysisResultSource.LIVE_AI_ANALYSIS,
                        "제육볶음",
                        "Spicy stir-fried pork",
                        "je-yuk-bokkeum",
                        "reason",
                        new BigDecimal("0.81"),
                        3L,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );

        mockMvc.perform(multipart("/api/v1/menus/analyze-image")
                        .file(image)
                        .with(authentication(userAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysisLogId").value(102))
                .andExpect(jsonPath("$.data.resultSource").value("LIVE_AI_ANALYSIS"));
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.authenticated(1L, "test-device", UserRole.USER);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
    }
}
