package com.mealguide.mealguide_api.mealcrawl.application.port;

public interface MealCrawlSchedulerLockPort {

    boolean tryAcquireLock();

    void releaseLock();
}
