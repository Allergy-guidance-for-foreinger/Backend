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
    private String analysisRetryCron = "0 0 1 * * *";
    private long schedulerLockKey = 20260416L;

    private String pythonBaseUrl = "http://localhost:8000";
    private String crawlPath = "/api/v1/crawl/meals";
    private String analysisPath = "/api/v1/menus/analyze";
    private String imageAnalysisPath = "/api/v1/python/menus/analyze-image";
    private String translationPath = "/api/v1/menus/translate";
    private String descriptionPath = "/api/v1/python/menus/describe/list";
    private String textTranslationPath = "/api/v1/translations";
    private String ingredientTranslationPath = "/api/v1/python/translations/list";
    private long weeklyMealCacheTtlSeconds = 86400L;
    private int aiAnalysisBatchSize = 10;
    private Integer aiAnalysisRetryBatchSize;
    private int aiAnalysisMaxAttemptCount = 3;
    private int translationBatchSize = 10;
    private Integer translationRetryBatchSize;
    private int translationMaxAttemptCount = 3;
    private int descriptionBatchSize = 7;
    private Integer descriptionRetryBatchSize;
    private int descriptionMaxAttemptCount = 3;
    private int ingredientTranslationBatchSize = 10;
    private int ingredientTranslationMaxBatchesPerRun = 5;
    private MenuImage menuImage = new MenuImage();

    private List<String> translationTargetLanguages = List.of("en");
    private List<String> descriptionTargetLanguages = List.of("ko", "en");
    private RiskPolicy riskPolicy = new RiskPolicy();

    public int getAiAnalysisBatchSize() {
        return aiAnalysisBatchSize > 0 ? aiAnalysisBatchSize : 10;
    }

    public int getAiAnalysisRetryBatchSize() {
        if (aiAnalysisRetryBatchSize == null || aiAnalysisRetryBatchSize <= 0) {
            return getAiAnalysisBatchSize();
        }
        return aiAnalysisRetryBatchSize;
    }

    public int getAiAnalysisMaxAttemptCount() {
        return aiAnalysisMaxAttemptCount > 0 ? aiAnalysisMaxAttemptCount : 3;
    }

    public int getTranslationBatchSize() {
        return translationBatchSize > 0 ? translationBatchSize : 10;
    }

    public int getTranslationRetryBatchSize() {
        if (translationRetryBatchSize == null || translationRetryBatchSize <= 0) {
            return getTranslationBatchSize();
        }
        return translationRetryBatchSize;
    }

    public int getTranslationMaxAttemptCount() {
        return translationMaxAttemptCount > 0 ? translationMaxAttemptCount : 3;
    }

    public int getDescriptionBatchSize() {
        return descriptionBatchSize > 0 ? descriptionBatchSize : 7;
    }

    public int getDescriptionRetryBatchSize() {
        if (descriptionRetryBatchSize == null || descriptionRetryBatchSize <= 0) {
            return getDescriptionBatchSize();
        }
        return descriptionRetryBatchSize;
    }

    public int getDescriptionMaxAttemptCount() {
        return descriptionMaxAttemptCount > 0 ? descriptionMaxAttemptCount : 3;
    }

    public int getIngredientTranslationBatchSize() {
        return ingredientTranslationBatchSize > 0 ? ingredientTranslationBatchSize : 10;
    }

    public int getIngredientTranslationMaxBatchesPerRun() {
        return ingredientTranslationMaxBatchesPerRun;
    }

    @Getter
    @Setter
    public static class RiskPolicy {
        private MetricPolicy allergy = new MetricPolicy();
        private MetricPolicy religious = new MetricPolicy();
    }

    @Getter
    @Setter
    public static class MetricPolicy {
        private boolean enabled = true;
        private java.math.BigDecimal cautionThreshold = new java.math.BigDecimal("0.50");
        private java.math.BigDecimal dangerThreshold = new java.math.BigDecimal("0.80");
        private String defaultWhenConfidenceMissing = "SAFE";
    }

    @Getter
    @Setter
    public static class MenuImage {
        private long maxFileSizeBytes = 10 * 1024 * 1024;
        private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
        private Firebase firebase = new Firebase();
    }

    @Getter
    @Setter
    public static class Firebase {
        private boolean enabled = false;
        private String bucketName;
        private String credentialsPath;
    }
}

