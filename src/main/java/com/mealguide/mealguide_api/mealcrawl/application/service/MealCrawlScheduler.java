package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealCrawlScheduler {

    private final MealCrawlProperties mealCrawlProperties;
    private final MealCrawlTargetService mealCrawlTargetService;
    private final MealCrawlOrchestrationService mealCrawlOrchestrationService;

    @Scheduled(cron = "${mealguide.mealcrawl.scheduler-cron:0 0 0 * * *}")
    public void runWeeklyCrawl() {
        if (!mealCrawlProperties.isSchedulerEnabled()) {
            return;
        }

        List<MealCrawlTarget> targets = mealCrawlTargetService.resolveWeeklyTargets(LocalDate.now());
        for (MealCrawlTarget target : targets) {
            try {
                mealCrawlOrchestrationService.crawlAndImport(target);
            } catch (Exception exception) {
                log.warn("Meal crawl failed for cafeteriaId={}", target.cafeteriaId(), exception);
            }
        }
    }
}

