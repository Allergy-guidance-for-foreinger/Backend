package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlSchedulerLockPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MealCrawlSchedulerTest {

    @Test
    void runAiRetryUsesAdvisoryLock() {
        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setSchedulerEnabled(true);

        MealCrawlSchedulerLockPort lockPort = mock(MealCrawlSchedulerLockPort.class);
        when(lockPort.tryAcquireLock()).thenReturn(true);

        MealCrawlTargetService targetService = mock(MealCrawlTargetService.class);
        MealCrawlOrchestrationService orchestrationService = mock(MealCrawlOrchestrationService.class);
        MenuAiAnalysisFollowUpService followUpService = mock(MenuAiAnalysisFollowUpService.class);
        MenuTranslationFollowUpService translationFollowUpService = mock(MenuTranslationFollowUpService.class);

        MealCrawlScheduler scheduler = new MealCrawlScheduler(
                properties,
                lockPort,
                targetService,
                orchestrationService,
                followUpService,
                translationFollowUpService
        );

        scheduler.runAiRetry();

        verify(lockPort).tryAcquireLock();
        verify(followUpService).processRetryPending(org.mockito.ArgumentMatchers.anyString());
        verify(translationFollowUpService).processRetryPending(org.mockito.ArgumentMatchers.anyString());
        verify(lockPort).releaseLock();
    }

    @Test
    void runAiRetrySkipsWhenLockNotAcquired() {
        MealCrawlProperties properties = new MealCrawlProperties();
        properties.setSchedulerEnabled(true);

        MealCrawlSchedulerLockPort lockPort = mock(MealCrawlSchedulerLockPort.class);
        when(lockPort.tryAcquireLock()).thenReturn(false);

        MealCrawlScheduler scheduler = new MealCrawlScheduler(
                properties,
                lockPort,
                mock(MealCrawlTargetService.class),
                mock(MealCrawlOrchestrationService.class),
                mock(MenuAiAnalysisFollowUpService.class),
                mock(MenuTranslationFollowUpService.class)
        );

        scheduler.runAiRetry();

        verify(lockPort).tryAcquireLock();
        verify(lockPort, never()).releaseLock();
    }
}
