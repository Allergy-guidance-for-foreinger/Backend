package com.mealguide.mealguide_api.mealcrawl.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mealguide.mealcrawl")
public class MealCrawlProperties {

    private boolean schedulerEnabled = false;
    private String schedulerCron = "0 0 4 * * *";
    private long schedulerLockKey = 20260416L;

    private String pythonBaseUrl = "http://localhost:8000";
    private String crawlPath = "/api/v1/crawl/meals";
    private String analysisPath = "/api/v1/menus/analyze";
    private String translationPath = "/api/v1/menus/translate";
    private long weeklyMealCacheTtlSeconds = 86400L;

    private List<String> translationTargetLanguages = List.of("en");
}

