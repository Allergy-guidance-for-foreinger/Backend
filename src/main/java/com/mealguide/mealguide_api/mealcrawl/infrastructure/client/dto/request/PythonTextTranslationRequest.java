package com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request;

public record PythonTextTranslationRequest(
        String sourceLang,
        String targetLang,
        String text
) {
}
