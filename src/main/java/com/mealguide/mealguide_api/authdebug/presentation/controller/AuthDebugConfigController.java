package com.mealguide.mealguide_api.authdebug.presentation.controller;

import com.mealguide.mealguide_api.authdebug.presentation.dto.response.AuthDebugConfigResponse;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.login.infrastructure.google.GoogleOAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "mealguide.auth-debug.enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/auth-debug")
public class AuthDebugConfigController {

    private final GoogleOAuthProperties googleOAuthProperties;

    @GetMapping("/config")
    public ResponseEntity<ResponseBody<AuthDebugConfigResponse>> config() {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(
                new AuthDebugConfigResponse(googleOAuthProperties.getClientId())
        ));
    }
}

