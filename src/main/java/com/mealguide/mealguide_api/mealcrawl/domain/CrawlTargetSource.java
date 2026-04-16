package com.mealguide.mealguide_api.mealcrawl.domain;

public record CrawlTargetSource(
        Long schoolId,
        Long cafeteriaId,
        String schoolName,
        String cafeteriaName,
        String sourceUrl
) {
}

