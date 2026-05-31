package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealCrawlTargetService {

    private final MealCrawlPersistencePort mealCrawlPersistencePort;

    @Transactional(readOnly = true)
    public List<MealCrawlTarget> resolveWeeklyTargets(LocalDate baseDate) {
        LocalDate startDate = resolveOperatingWeekStartDate(baseDate);
        LocalDate endDate = startDate.plusDays(4);

        return mealCrawlPersistencePort.findCrawlTargets().stream()
                .map(source -> toTarget(source, startDate, endDate))
                .toList();
    }

    private MealCrawlTarget toTarget(CrawlTargetSource source, LocalDate startDate, LocalDate endDate) {
        return new MealCrawlTarget(
                source.schoolId(),
                source.cafeteriaId(),
                source.schoolName(),
                source.cafeteriaName(),
                source.sourceUrl(),
                startDate,
                endDate
        );
    }

    private LocalDate resolveOperatingWeekStartDate(LocalDate baseDate) {
        if (baseDate == null) {
            throw new IllegalArgumentException("baseDate must not be null");
        }
        DayOfWeek dayOfWeek = baseDate.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return baseDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }
        return WeekStartDateNormalizer.normalize(baseDate);
    }
}

