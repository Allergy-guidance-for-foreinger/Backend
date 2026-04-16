package com.mealguide.mealguide_api.settings.application.service;

import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsMasterQueryPort settingsMasterQueryPort;

    @Transactional(readOnly = true)
    public List<LanguageOption> getLanguages() {
        return settingsMasterQueryPort.findLanguages();
    }

    @Transactional(readOnly = true)
    public List<AllergyOption> getAllergies(String langCode) {
        return settingsMasterQueryPort.findAllergies(normalize(langCode));
    }

    @Transactional(readOnly = true)
    public List<ReligiousRestrictionOption> getReligions(String langCode) {
        return settingsMasterQueryPort.findReligiousRestrictions(normalize(langCode));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

