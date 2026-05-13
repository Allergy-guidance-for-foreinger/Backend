package com.mealguide.mealguide_api.mealcrawl.domain;

import java.util.Arrays;

public enum MenuSpicyLevel {
    LEVEL_0(0L),
    LEVEL_1(1L),
    LEVEL_2(2L),
    LEVEL_3(3L),
    LEVEL_4(4L),
    LEVEL_5(5L);

    private final long value;

    MenuSpicyLevel(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }

    public static MenuSpicyLevel fromValue(Long value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(level -> level.value == value)
                .findFirst()
                .orElse(null);
    }
}
