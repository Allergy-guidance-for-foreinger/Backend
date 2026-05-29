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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealCrawlOrchestrationService {

    private final PythonMealClientPort pythonMealClientPort;
    private final MealCrawlPersistencePort mealCrawlPersistencePort;
    private final MealImportService mealImportService;
    private final WeeklyMealCacheRefreshService weeklyMealCacheRefreshService;
    private final MenuAiAnalysisFollowUpService menuAiAnalysisFollowUpService;
    private final MenuTranslationFollowUpService menuTranslationFollowUpService;
    private final MenuDescriptionFollowUpService menuDescriptionFollowUpService;
    private final IngredientTranslationFollowUpService ingredientTranslationFollowUpService;

    public void crawlAndImport(MealCrawlTarget target) {
        crawlAndImport("manual", target);
    }

    public void crawlAndImport(String runId, MealCrawlTarget target) {
        Instant startedAt = Instant.now();
        log.info(
                "event=START stage=crawl_orchestration runId={} schoolId={} cafeteriaId={} weekStartDate={} weekEndDate={}",
                runId,
                target.schoolId(),
                target.cafeteriaId(),
                target.startDate(),
                target.endDate()
        );

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
            log.info(
                    "event=END stage=crawl_import runId={} schoolId={} cafeteriaId={} weekStartDate={} importedMenuCount={} menusNeedingAnalysisCount={} menusNeedingTranslationCount={} result=SUCCESS",
                    runId,
                    target.schoolId(),
                    target.cafeteriaId(),
                    target.startDate(),
                    importResult.importedMenuIds().size(),
                    importResult.menusNeedingAnalysis().size(),
                    importResult.menusNeedingTranslation().size()
            );
            mealCrawlPersistencePort.markCrawlHistorySuccess(historyId, LocalDateTime.now());
        } catch (Exception exception) {
            mealCrawlPersistencePort.markCrawlHistoryFailure(historyId, shorten(exception.getMessage()), LocalDateTime.now());
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.error(
                    "event=FAIL stage=crawl_orchestration runId={} schoolId={} cafeteriaId={} weekStartDate={} durationMs={} errorType={} message={}",
                    runId,
                    target.schoolId(),
                    target.cafeteriaId(),
                    target.startDate(),
                    durationMs,
                    exception.getClass().getSimpleName(),
                    shorten(exception.getMessage()),
                    exception
            );
            throw exception;
        }

        try {
            weeklyMealCacheRefreshService.refreshWeeklyMealCache(runId, target.schoolId(), target.cafeteriaId(), target.startDate());
        } catch (Exception exception) {
            log.warn(
                    "event=FAIL stage=cache_refresh runId={} schoolId={} cafeteriaId={} weekStartDate={} errorType={} message={}",
                    runId,
                    target.schoolId(),
                    target.cafeteriaId(),
                    target.startDate(),
                    exception.getClass().getSimpleName(),
                    shorten(exception.getMessage()),
                    exception
            );
        }

        try {
            menuAiAnalysisFollowUpService.process(runId, target.schoolId(), target.cafeteriaId(), target.startDate(), importResult);
        } catch (Exception exception) {
            log.warn(
                    "event=FAIL stage=ai_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} errorType={} message={}",
                    runId,
                    target.schoolId(),
                    target.cafeteriaId(),
                    target.startDate(),
                    exception.getClass().getSimpleName(),
                    shorten(exception.getMessage()),
                    exception
            );
        }

        try {
            menuTranslationFollowUpService.process(runId, target.schoolId(), target.cafeteriaId(), target.startDate(), importResult);
        } catch (Exception exception) {
            log.warn(
                    "event=FAIL stage=translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} errorType={} message={}",
                    runId,
                    target.schoolId(),
                    target.cafeteriaId(),
                    target.startDate(),
                    exception.getClass().getSimpleName(),
                    shorten(exception.getMessage()),
                    exception
            );
        }

        try {
            menuDescriptionFollowUpService.process(runId, target.schoolId(), target.cafeteriaId(), target.startDate(), importResult);
        } catch (Exception exception) {
            log.warn(
                    "event=FAIL stage=description_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} errorType={} message={}",
                    runId,
                    target.schoolId(),
                    target.cafeteriaId(),
                    target.startDate(),
                    exception.getClass().getSimpleName(),
                    shorten(exception.getMessage()),
                    exception
            );
        }

        try {
            ingredientTranslationFollowUpService.process(runId);
        } catch (Exception exception) {
            log.warn(
                    "event=FAIL stage=ingredient_translation_followup runId={} schoolId={} cafeteriaId={} weekStartDate={} errorType={} message={}",
                    runId,
                    target.schoolId(),
                    target.cafeteriaId(),
                    target.startDate(),
                    exception.getClass().getSimpleName(),
                    shorten(exception.getMessage()),
                    exception
            );
        }

        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info(
                "event=END stage=crawl_orchestration runId={} schoolId={} cafeteriaId={} weekStartDate={} durationMs={} result=SUCCESS",
                runId,
                target.schoolId(),
                target.cafeteriaId(),
                target.startDate(),
                durationMs
        );
    }

    private String shorten(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown failure";
        }
        String normalized = message.trim();
        return normalized.length() > 1000 ? normalized.substring(0, 1000) : normalized;
    }
}

