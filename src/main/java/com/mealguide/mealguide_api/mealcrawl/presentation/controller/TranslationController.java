package com.mealguide.mealguide_api.mealcrawl.presentation.controller;

import com.mealguide.mealguide_api.global.base.dto.ResponseBody;
import com.mealguide.mealguide_api.global.base.dto.ResponseUtils;
import com.mealguide.mealguide_api.mealcrawl.application.service.TranslationService;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.request.TranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.TranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.swagger.TranslationApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RequestMapping("/api/v1")
public class TranslationController implements TranslationApi {

    private final TranslationService translationService;

    @PostMapping("/translations")
    public ResponseEntity<ResponseBody<TranslationResponse>> translate(
            @Valid @RequestBody TranslationRequest request
    ) {
        TranslationResponse response = translationService.translate(
                request.sourceLang(),
                request.targetLang(),
                request.text()
        );
        return ResponseEntity.ok(ResponseUtils.createSuccessResponse(response));
    }
}
