package com.mealguide.mealguide_api.mealcrawl.application.service;

import com.mealguide.mealguide_api.mealcrawl.application.dto.MealCrawlTarget;
import com.mealguide.mealguide_api.mealcrawl.application.dto.WeeklyMealCacheRow;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonCrawledMenuDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonDailyMealDto;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.client.dto.response.PythonMealCrawlResponse;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter.MealCrawlPersistenceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        MealCrawlPersistenceAdapter.class,
        MealImportService.class,
        MealImportServiceDatabaseIntegrationTest.Config.class
})
class MealImportServiceDatabaseIntegrationTest {

    @Autowired
    private MealImportService mealImportService;

    @Autowired
    private MealCrawlPersistenceAdapter mealCrawlPersistenceAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long schoolId;
    private Long cafeteriaId;

    @BeforeEach
    void setUp() {
        schoolId = insertSchool("Test School", "https://example.com");
        cafeteriaId = insertCafeteria(schoolId, "Main Cafeteria");
    }

    @Test
    void importMealsIsDuplicateSafeAndMealMenuIsUpsertedOnSameDisplayOrder() {
        LocalDate mealDate = LocalDate.of(2026, 4, 15);
        MealCrawlTarget target = new MealCrawlTarget(
                schoolId,
                cafeteriaId,
                "Test School",
                "Main Cafeteria",
                "https://example.com",
                mealDate,
                mealDate.plusDays(6)
        );

        PythonMealCrawlResponse first = new PythonMealCrawlResponse(
                "Test School",
                "Main Cafeteria",
                "https://example.com",
                mealDate,
                mealDate.plusDays(6),
                List.of(new PythonDailyMealDto(
                        mealDate,
                        "LUNCH",
                        List.of(new PythonCrawledMenuDto("A", 1, "Kimchi Stew"))
                ))
        );

        PythonMealCrawlResponse second = new PythonMealCrawlResponse(
                "Test School",
                "Main Cafeteria",
                "https://example.com",
                mealDate,
                mealDate.plusDays(6),
                List.of(new PythonDailyMealDto(
                        mealDate,
                        "LUNCH",
                        List.of(new PythonCrawledMenuDto("A", 1, "Soybean Stew"))
                ))
        );

        mealImportService.importMeals(target, first);
        mealImportService.importMeals(target, second);
        entityManager.flush();
        entityManager.clear();

        Long mealScheduleCount = jdbcTemplate.queryForObject("select count(*) from meal_schedule", Long.class);
        Long mealMenuCount = jdbcTemplate.queryForObject("select count(*) from meal_menu", Long.class);
        Long menuCount = jdbcTemplate.queryForObject("select count(*) from menu", Long.class);

        assertThat(mealScheduleCount).isEqualTo(1L);
        assertThat(mealMenuCount).isEqualTo(1L);
        assertThat(menuCount).isEqualTo(2L);

        String selectedMenuName = jdbcTemplate.queryForObject(
                """
                select m.name
                from meal_menu mm
                join menu m on m.id = mm.menu_id
                where mm.display_order = 1
                """,
                String.class
        );

        assertThat(selectedMenuName).isEqualTo("Soybean Stew");
    }

    @Test
    void findWeeklyMealsForCacheOrdersMealTypeByMealTime() {
        LocalDate mealDate = LocalDate.of(2026, 4, 15);
        MealCrawlTarget target = new MealCrawlTarget(
                schoolId,
                cafeteriaId,
                "Test School",
                "Main Cafeteria",
                "https://example.com",
                mealDate,
                mealDate.plusDays(6)
        );

        PythonMealCrawlResponse response = new PythonMealCrawlResponse(
                "Test School",
                "Main Cafeteria",
                "https://example.com",
                mealDate,
                mealDate.plusDays(6),
                List.of(
                        new PythonDailyMealDto(
                                mealDate,
                                "LUNCH",
                                List.of(new PythonCrawledMenuDto("A", 1, "Lunch Menu"))
                        ),
                        new PythonDailyMealDto(
                                mealDate,
                                "DINNER",
                                List.of(new PythonCrawledMenuDto("A", 1, "Dinner Menu"))
                        ),
                        new PythonDailyMealDto(
                                mealDate,
                                "BREAKFAST",
                                List.of(new PythonCrawledMenuDto("A", 1, "Breakfast Menu"))
                        )
                )
        );

        mealImportService.importMeals(target, response);
        entityManager.flush();
        entityManager.clear();

        List<WeeklyMealCacheRow> rows = mealCrawlPersistenceAdapter.findWeeklyMealsForCache(cafeteriaId, mealDate, mealDate);
        List<String> mealTypes = rows.stream().map(WeeklyMealCacheRow::mealType).toList();

        assertThat(mealTypes).containsExactly("BREAKFAST", "LUNCH", "DINNER");
    }

    private Long insertSchool(String name, String sourceUrl) {
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into school(name, source_url, created_at) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, sourceUrl);
            ps.setObject(3, LocalDateTime.now());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private Long insertCafeteria(Long schoolId, String name) {
        var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into cafeteria(school_id, name, created_at) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, schoolId);
            ps.setString(2, name);
            ps.setObject(3, LocalDateTime.now());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    @TestConfiguration
    static class Config {
        @Bean
        MealCrawlProperties mealCrawlProperties() {
            MealCrawlProperties properties = new MealCrawlProperties();
            properties.setTranslationTargetLanguages(List.of("en"));
            return properties;
        }
    }
}
