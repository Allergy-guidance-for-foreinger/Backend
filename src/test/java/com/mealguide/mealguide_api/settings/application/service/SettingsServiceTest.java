package com.mealguide.mealguide_api.settings.application.service;

import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsServiceTest {

    private final FakeSettingsMasterQueryPort settingsMasterQueryPort = new FakeSettingsMasterQueryPort();
    private final SettingsService settingsService = new SettingsService(settingsMasterQueryPort);

    @Test
    void getLanguagesReturnsSelectableLanguageOptions() {
        assertThat(settingsService.getLanguages())
                .containsExactly(new LanguageOption("en", "?ÅÏñ¥", "English"));
    }

    @Test
    void getAllergiesReturnsTranslatedOrFallbackNamesFromPort() {
        assertThat(settingsService.getAllergies("en"))
                .containsExactly(new AllergyOption("EGG", "Egg", 1));
    }

    @Test
    void getReligionsReturnsTranslatedOrFallbackNamesFromPort() {
        assertThat(settingsService.getReligions("en"))
                .containsExactly(new ReligiousRestrictionOption("HALAL", "Halal"));
    }

    private static class FakeSettingsMasterQueryPort implements SettingsMasterQueryPort {
        @Override
        public List<LanguageOption> findLanguages() {
            return List.of(new LanguageOption("en", "?ÅÏñ¥", "English"));
        }

        @Override
        public boolean existsLanguageCode(String languageCode) {
            return true;
        }

        @Override
        public List<AllergyOption> findAllergies(String langCode) {
            return List.of(new AllergyOption("EGG", "Egg", 1));
        }

        @Override
        public boolean existsAllAllergyCodes(Set<String> allergyCodes) {
            return true;
        }

        @Override
        public List<ReligiousRestrictionOption> findReligiousRestrictions(String langCode) {
            return List.of(new ReligiousRestrictionOption("HALAL", "Halal"));
        }

        @Override
        public boolean existsReligiousCode(String religiousCode) {
            return true;
        }
    }
}

