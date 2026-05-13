package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlSchedulerLockPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealCrawlScheduler {

    private final MealCrawlProperties mealCrawlProperties;
    private final MealCrawlSchedulerLockPort mealCrawlSchedulerLockPort;
    private final MealCrawlTargetService mealCrawlTargetService;
    private final MealCrawlOrchestrationService mealCrawlOrchestrationService;

    @Scheduled(cron = "${mealguide.mealcrawl.scheduler-cron:0 0 0 * * *}")
    public void runWeeklyCrawl() {
        if (!mealCrawlProperties.isSchedulerEnabled()) {
            return;
        }

        if (!mealCrawlSchedulerLockPort.tryAcquireLock()) {
            log.info("Skipped meal crawl scheduling because another instance holds the lock");
            return;
        }

        String runId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        log.info("event=START stage=scheduler_run runId={} trigger=scheduler baseDate={}", runId, LocalDate.now());

        int successCafeteriaCount = 0;
        int failCafeteriaCount = 0;
        int targetCount = 0;

        try {
            List<MealCrawlTarget> targets = mealCrawlTargetService.resolveWeeklyTargets(LocalDate.now());
            targetCount = targets.size();
            for (MealCrawlTarget target : targets) {
                try {
                    mealCrawlOrchestrationService.crawlAndImport(runId, target);
                    successCafeteriaCount++;
                } catch (Exception exception) {
                    failCafeteriaCount++;
                    log.warn(
                            "event=FAIL stage=scheduler_cafeteria runId={} cafeteriaId={} errorType={} message={}",
                            runId,
                            target.cafeteriaId(),
                            exception.getClass().getSimpleName(),
                            exception.getMessage(),
                            exception
                    );
                }
            }
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.info(
                    "event=END stage=scheduler_run runId={} targetCafeteriaCount={} successCafeteriaCount={} failCafeteriaCount={} totalDurationMs={} result={}",
                    runId,
                    targetCount,
                    successCafeteriaCount,
                    failCafeteriaCount,
                    durationMs,
                    failCafeteriaCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS"
            );
        } catch (Exception exception) {
            long durationMs = Duration.between(startedAt, Instant.now()).toMillis();
            log.error(
                    "event=FAIL stage=scheduler_run runId={} targetCafeteriaCount={} successCafeteriaCount={} failCafeteriaCount={} totalDurationMs={} errorType={} message={}",
                    runId,
                    targetCount,
                    successCafeteriaCount,
                    failCafeteriaCount,
                    durationMs,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
            throw exception;
        } finally {
            mealCrawlSchedulerLockPort.releaseLock();
        }
    }
}

