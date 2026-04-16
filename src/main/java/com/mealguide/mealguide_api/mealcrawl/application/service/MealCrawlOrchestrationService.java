package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.application.dto.MealImportResult;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.application.port.PythonMealClientPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.request.PythonMealCrawlRequest;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealCrawlOrchestrationService {

    private final PythonMealClientPort pythonMealClientPort;
    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final MealImportService mealImportService;
    private final MenuAiAnalysisFollowUpService menuAiAnalysisFollowUpService;
    private final MenuTranslationFollowUpService menuTranslationFollowUpService;

    public void crawlAndImport(MealCrawlTarget target) {
        Long historyId = mealCrawlPersistencePort.startCrawlHistory(
                target.cafeteriaId(),
                target.startDate(),
                target.endDate(),
                LocalDateTime.now()
        );

        MealImportResult importResult;
        try {
            PythonMealCrawlResponse crawlResponse = pythonMealClientPort.crawlMeals(new PythonMealCrawlRequest(
                    target.schoolName(),
                    target.cafeteriaName(),
                    target.sourceUrl(),
                    target.startDate(),
                    target.endDate()
            ));

            importResult = mealImportService.importMeals(target, crawlResponse);
            mealCrawlPersistencePort.markCrawlHistorySuccess(historyId, LocalDateTime.now());
        } catch (Exception exception) {
            mealCrawlPersistencePort.markCrawlHistoryFailure(historyId, shorten(exception.getMessage()), LocalDateTime.now());
            throw exception;
        }

        try {
            menuAiAnalysisFollowUpService.process(importResult);
        } catch (Exception exception) {
            log.warn("Menu AI analysis follow-up failed for cafeteriaId={}", target.cafeteriaId(), exception);
        }

        try {
            menuTranslationFollowUpService.process(importResult);
        } catch (Exception exception) {
            log.warn("Menu translation follow-up failed for cafeteriaId={}", target.cafeteriaId(), exception);
        }
    }

    private String shorten(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown failure";
        }
        String normalized = message.trim();
        return normalized.length() > 1000 ? normalized.substring(0, 1000) : normalized;
    }
}

