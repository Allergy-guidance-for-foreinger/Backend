package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealguide.mealguide_api.global.base.exception.ExternalApiException;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.PythonMealClientException;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonTextTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonTextTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.presentation.dto.response.TranslationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TranslationServiceTest {

    @Test
    void translateReturnsTranslatedText() {
        PythonMealClientPort port = mock(PythonMealClientPort.class);
        when(port.translateText(any(PythonTextTranslationRequest.class)))
                .thenReturn(new PythonTextTranslationResponse("hello"));
        TranslationService service = new TranslationService(port, new ObjectMapper());

        TranslationResponse response = service.translate("ko", "en", "안녕하세요");

        assertThat(response.translatedText()).isEqualTo("hello");
    }

    @Test
    void translateThrowsExternalApiExceptionWhenPythonFails() {
        PythonMealClientPort port = mock(PythonMealClientPort.class);
        when(port.translateText(any(PythonTextTranslationRequest.class)))
                .thenThrow(new PythonMealClientException(
                        "failed",
                        502,
                        "{\"code\":\"PYM_123\",\"msg\":\"python failed\"}",
                        true,
                        null
                ));
        TranslationService service = new TranslationService(port, new ObjectMapper());

        assertThatThrownBy(() -> service.translate("ko", "en", "안녕하세요"))
                .isInstanceOf(ExternalApiException.class)
                .satisfies(ex -> {
                    ExternalApiException exception = (ExternalApiException) ex;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getCode()).isEqualTo("PYM_123");
                    assertThat(exception.getMessage()).isEqualTo("python failed");
                });
    }

    @Test
    void translateThrowsServiceExceptionWhenTranslatedTextIsBlank() {
        PythonMealClientPort port = mock(PythonMealClientPort.class);
        when(port.translateText(any(PythonTextTranslationRequest.class)))
                .thenReturn(new PythonTextTranslationResponse(" "));
        TranslationService service = new TranslationService(port, new ObjectMapper());

        assertThatThrownBy(() -> service.translate("ko", "en", "안녕하세요"))
                .isInstanceOf(ServiceException.class);
    }
}
