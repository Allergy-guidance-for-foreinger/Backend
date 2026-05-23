package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ExternalApiException;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.PythonMealClientException;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonTextTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonTextTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.TranslationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final PythonMealClientPort pythonMealClientPort;
    private final ObjectMapper objectMapper;

    public TranslationResponse translate(String sourceLang, String targetLang, String text) {
        try {
            String normalizedSourceLang = normalizeLanguageCode(sourceLang);
            String normalizedTargetLang = normalizeLanguageCode(targetLang);
            PythonTextTranslationResponse response = pythonMealClientPort.translateText(
                    new PythonTextTranslationRequest(normalizedSourceLang, normalizedTargetLang, text)
            );
            if (response == null || response.translatedText() == null || response.translatedText().isBlank()) {
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }
            return new TranslationResponse(response.translatedText());
        } catch (PythonMealClientException e) {
            throw mapPythonException(e);
        }
    }

    private RuntimeException mapPythonException(PythonMealClientException e) {
        String code = "PYM_500";
        String msg = "Python API call failed.";
        String responseBody = e.getResponseBody();
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                var node = objectMapper.readTree(responseBody);
                code = node.path("code").asText(code);
                msg = node.path("msg").asText(msg);
            } catch (Exception ignored) {
                // keep default code/message
            }
        }
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        if (e.getHttpStatus() != null) {
            HttpStatus resolved = HttpStatus.resolve(e.getHttpStatus());
            if (resolved != null) {
                status = resolved;
            }
        }
        return new ExternalApiException(
                status,
                (code == null || code.isBlank()) ? "PYM_500" : code,
                (msg == null || msg.isBlank()) ? "Python API call failed." : msg
        );
    }

    private String normalizeLanguageCode(String languageCode) {
        return languageCode == null ? null : languageCode.trim().toLowerCase(Locale.ROOT);
    }
}
