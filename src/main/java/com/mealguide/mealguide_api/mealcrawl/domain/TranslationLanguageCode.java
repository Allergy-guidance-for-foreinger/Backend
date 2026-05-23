package com.mealguide.mealguide_api.mealcrawl.domain;

import java.util.Arrays;
import java.util.Locale;

public enum TranslationLanguageCode {
    KO("ko"),
    EN("en");

    private final String code;

    TranslationLanguageCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static boolean isSupported(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .anyMatch(languageCode -> languageCode.code.equals(normalized));
    }
}
