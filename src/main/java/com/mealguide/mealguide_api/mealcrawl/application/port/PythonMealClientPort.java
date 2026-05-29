package com.mealguide.mealguide_api.mealcrawl.application.port;

import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuAnalysisRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMenuDescriptionRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuDescriptionResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMenuImageAnalysisResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonTextTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonIngredientTranslationRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonIngredientTranslationResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonTextTranslationResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PythonMealClientPort {
    PythonMealCrawlResponse crawlMeals(PythonMealCrawlRequest request);

    PythonMenuAnalysisResponse analyzeMenus(PythonMenuAnalysisRequest request);

    PythonMenuTranslationResponse translateMenus(PythonMenuTranslationRequest request);

    default PythonMenuDescriptionResponse describeMenus(PythonMenuDescriptionRequest request) {
        return new PythonMenuDescriptionResponse(java.util.List.of());
    }

    PythonMenuImageAnalysisResponse analyzeImage(MultipartFile image, String langCode);

    PythonTextTranslationResponse translateText(PythonTextTranslationRequest request);

    PythonIngredientTranslationResponse translateIngredients(PythonIngredientTranslationRequest request);
}


