package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlPersistencePort;
import com.mealguide.mealguide_api.mealcrawl.domain.CrawlTargetSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MealCrawlTargetServiceTest {

    @Test
    void resolveWeeklyTargetsNormalizesStartDateToMonday() {
        MealCrawlPersistencePort persistencePort = mock(MealCrawlPersistencePort.class);
        when(persistencePort.findCrawlTargets()).thenReturn(List.of(
                new CrawlTargetSource(1L, 10L, "School", "Main", "http://source")
        ));

        MealCrawlTargetService service = new MealCrawlTargetService(persistencePort);

        List<MealCrawlTarget> targets = service.resolveWeeklyTargets(LocalDate.of(2026, 4, 22));

        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).startDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(targets.get(0).endDate()).isEqualTo(LocalDate.of(2026, 4, 26));
    }
}
