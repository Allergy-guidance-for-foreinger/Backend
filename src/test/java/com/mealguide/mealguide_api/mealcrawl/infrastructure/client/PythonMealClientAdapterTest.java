package com.mealguide.mealguide_api.mealcrawl.infrastructure.client;

import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisTargetDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonTextTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonTextTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PythonMealClientAdapterTest {

    @Test
    void crawlMealsMapsRequestAndResponse() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setPythonBaseUrl("http://python");
        properties.setCrawlPath("/crawl");

        PythonMealCrawlRequest request = new PythonMealCrawlRequest("school", "cafeteria", "url", LocalDate.now(), LocalDate.now().plusDays(6));
        PythonMealCrawlResponse expected = new PythonMealCrawlResponse("school", "cafeteria", "url", request.startDate(), request.endDate(), List.of());

        when(builder.baseUrl("http://python")).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/crawl")).thenReturn(bodySpec);
        when(bodySpec.body(request)).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PythonMealCrawlResponse.class)).thenReturn(expected);

        PythonMealClientAdapter adapter = new PythonMealClientAdapter(builder, properties);
        PythonMealCrawlResponse actual = adapter.crawlMeals(request);

        assertThat(actual).isEqualTo(expected);
        verify(bodySpec).body(request);
        verify(responseSpec).body(PythonMealCrawlResponse.class);
        verify(restClient).post();
    }

    @Test
    void analyzeMenusPreservesExceptionContext() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setPythonBaseUrl("http://python");
        properties.setAnalysisPath("/analyze");

        PythonMenuAnalysisRequest request = new PythonMenuAnalysisRequest(List.of(
                new PythonMenuAnalysisTargetDto(1L, "Menu")
        ));

        when(builder.baseUrl("http://python")).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/analyze")).thenReturn(bodySpec);
        when(bodySpec.body(request)).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse.class))
                .thenThrow(new ResourceAccessException("timeout"));

        PythonMealClientAdapter adapter = new PythonMealClientAdapter(builder, properties);

        assertThatThrownBy(() -> adapter.analyzeMenus(request))
                .isInstanceOf(PythonMealClientException.class)
                .hasMessageContaining("analysis request failed")
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void translateTextMapsRequestAndResponse() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setPythonBaseUrl("http://python");
        properties.setTextTranslationPath("/api/v1/translations");

        PythonTextTranslationRequest request = new PythonTextTranslationRequest("ko", "en", "안녕하세요");
        PythonTextTranslationResponse expected = new PythonTextTranslationResponse("hello");

        when(builder.baseUrl("http://python")).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/v1/translations")).thenReturn(bodySpec);
        when(bodySpec.body(request)).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PythonTextTranslationResponse.class)).thenReturn(expected);

        PythonMealClientAdapter adapter = new PythonMealClientAdapter(builder, properties);
        PythonTextTranslationResponse actual = adapter.translateText(request);

        assertThat(actual).isEqualTo(expected);
        verify(bodySpec).body(request);
        verify(responseSpec).body(PythonTextTranslationResponse.class);
    }
}
