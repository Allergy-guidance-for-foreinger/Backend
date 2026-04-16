package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealCrawlTargetService {

    private final MealCrawlPersistencePort mealCrawlPersistencePort;

    @Transactional(readOnly = true)
    public List<MealCrawlTarget> resolveWeeklyTargets(LocalDate baseDate) {
        LocalDate startDate = baseDate;
        LocalDate endDate = baseDate.plusDays(6);

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
}

