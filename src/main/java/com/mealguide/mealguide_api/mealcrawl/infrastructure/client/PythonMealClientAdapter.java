package com.mealguide.mealguide_api.mealcrawl.infrastructure.client;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PythonMealClientAdapter implements PythonMealClientPort {

    private final RestClient restClient;
    private final MealCrawlProperties mealCrawlProperties;

    public PythonMealClientAdapter(RestClient.Builder restClientBuilder, MealCrawlProperties mealCrawlProperties) {
        this.restClient = restClientBuilder.baseUrl(mealCrawlProperties.getPythonBaseUrl()).build();
        this.mealCrawlProperties = mealCrawlProperties;
    }

    @Override
    public PythonMealCrawlResponse crawlMeals(PythonMealCrawlRequest request) {
        try {
            PythonMealCrawlResponse response = restClient.post()
                    .uri(mealCrawlProperties.getCrawlPath())
                    .body(request)
                    .retrieve()
                    .body(PythonMealCrawlResponse.class);

            if (response == null) {
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }
            return response;
        } catch (RestClientException exception) {
            throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
        }
    }

    @Override
    public PythonMenuAnalysisResponse analyzeMenus(PythonMenuAnalysisRequest request) {
        try {
            PythonMenuAnalysisResponse response = restClient.post()
                    .uri(mealCrawlProperties.getAnalysisPath())
                    .body(request)
                    .retrieve()
                    .body(PythonMenuAnalysisResponse.class);

            if (response == null) {
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }
            return response;
        } catch (RestClientException exception) {
            throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
        }
    }

    @Override
    public PythonMenuTranslationResponse translateMenus(PythonMenuTranslationRequest request) {
        try {
            PythonMenuTranslationResponse response = restClient.post()
                    .uri(mealCrawlProperties.getTranslationPath())
                    .body(request)
                    .retrieve()
                    .body(PythonMenuTranslationResponse.class);

            if (response == null) {
                throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
            }
            return response;
        } catch (RestClientException exception) {
            throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR);
        }
    }
}

