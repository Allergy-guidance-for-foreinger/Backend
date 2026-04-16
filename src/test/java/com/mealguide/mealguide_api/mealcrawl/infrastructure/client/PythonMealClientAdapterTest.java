package com.mealguide.mealguide_api.mealcrawl.infrastructure.client;

import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}

