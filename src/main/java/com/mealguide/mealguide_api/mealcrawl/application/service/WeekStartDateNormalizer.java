package com.mealguide.mealguide_api.mealcrawl.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public final class WeekStartDateNormalizer {

    private WeekStartDateNormalizer() {
    }

    public static LocalDate normalize(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
