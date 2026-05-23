package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseMenuImageStorageAdapterTest {

    @Test
    void buildObjectPathFollowsRequiredFormat() {
        FirebaseMenuImageStorageAdapter adapter = new FirebaseMenuImageStorageAdapter(new MealCrawlProperties());
        String path = adapter.buildObjectPath(15L, "jpg");

        assertThat(path).startsWith("menu-analysis/15/");
        assertThat(path).endsWith(".jpg");
        assertThat(path).matches("^menu-analysis/15/[0-9a-fA-F\\-]{36}\\.jpg$");
    }
}

