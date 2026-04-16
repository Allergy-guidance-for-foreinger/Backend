package com.mealguide.mealguide_api.login.presentation.controller;

import com.mealguide.mealguide_api.global.auth.annotation.CurrentUserId;
import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.login.application.service.LoginService;
import com.mealguide.mealguide_api.login.presentation.dto.request.LoginRequest;
import com.mealguide.mealguide_api.login.presentation.dto.request.LogoutRequest;
import com.mealguide.mealguide_api.login.presentation.dto.request.RefreshTokenRequest;
import com.mealguide.mealguide_api.login.presentation.dto.response.AuthResponse;
import com.mealguide.mealguide_api.login.presentation.swagger.AuthApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController implements AuthApi {

    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<ResponseBody<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(AuthResponse.from(loginService.login(request.idToken(), request.deviceId()))));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseBody<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(AuthResponse.from(loginService.refresh(request.refreshToken()))));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseBody<Void>> logout(
            @CurrentUserId Long currentUserId,
            @Valid @RequestBody LogoutRequest request
    ) {
        loginService.logout(currentUserId, request.refreshToken());
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse());
    }
}

