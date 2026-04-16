package com.mealguide.mealguide_api.mealcrawl.application.port;

import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;

public interface PythonMealClientPort {
    PythonMealCrawlResponse crawlMeals(PythonMealCrawlRequest request);

    PythonMenuAnalysisResponse analyzeMenus(PythonMenuAnalysisRequest request);

    PythonMenuTranslationResponse translateMenus(PythonMenuTranslationRequest request);
}


