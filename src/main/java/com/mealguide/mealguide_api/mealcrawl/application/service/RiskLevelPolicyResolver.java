package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.domain.MenuRiskLevel;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RiskLevelPolicyResolver {

    private final MealCrawlProperties properties;

    public RiskLevelPolicyResolver(MealCrawlProperties properties) {
        this.properties = properties;
    }

    public MenuRiskLevel resolveAllergy(boolean hasMatch, BigDecimal confidence) {
        return resolve(hasMatch, confidence, properties.getRiskPolicy().getAllergy());
    }

    public MenuRiskLevel resolveReligious(boolean hasMatch, BigDecimal confidence) {
        return resolve(hasMatch, confidence, properties.getRiskPolicy().getReligious());
    }

    private MenuRiskLevel resolve(
            boolean hasMatch,
            BigDecimal confidence,
            MealCrawlProperties.MetricPolicy policy
    ) {
        if (!hasMatch || !policy.isEnabled()) {
            return MenuRiskLevel.SAFE;
        }
        if (confidence == null) {
            return parseDefault(policy.getDefaultWhenConfidenceMissing());
        }
        if (confidence.compareTo(policy.getDangerThreshold()) >= 0) {
            return MenuRiskLevel.DANGER;
        }
        if (confidence.compareTo(policy.getCautionThreshold()) >= 0) {
            return MenuRiskLevel.CAUTION;
        }
        return MenuRiskLevel.SAFE;
    }

    private MenuRiskLevel parseDefault(String value) {
        if (value == null || value.isBlank()) {
            return MenuRiskLevel.CAUTION;
        }
        try {
            MenuRiskLevel parsed = MenuRiskLevel.valueOf(value.trim().toUpperCase());
            return parsed == MenuRiskLevel.UNKNOWN ? MenuRiskLevel.CAUTION : parsed;
        } catch (IllegalArgumentException exception) {
            return MenuRiskLevel.CAUTION;
        }
    }
}
