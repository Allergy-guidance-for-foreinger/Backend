package com.mealguide.mealguide_api.settings.application.service;

import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.CountryOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import com.mealguide.mealguide_api.settings.domain.SchoolOption;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.AllergyOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.CountryOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.CountryOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.LanguageOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.ReligionOptionsResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.SchoolOptionItemResponse;
import com.mealguide.mealguide_api.settings.presentation.dto.response.SchoolOptionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsMasterQueryPort settingsMasterQueryPort;
    private final UserPreferenceService userPreferenceService;

    @Transactional(readOnly = true)
    public LanguageOptionsResponse getLanguageOptions() {
        List<LanguageOptionItemResponse> languages = getLanguages().stream()
                .map(LanguageOptionItemResponse::from)
                .toList();
        return new LanguageOptionsResponse(languages);
    }

    @Transactional(readOnly = true)
    public ReligionOptionsResponse getReligionOptions(Long userId) {
        String languageCode = userPreferenceService.getLanguage(userId);
        List<ReligionOptionItemResponse> religions = getReligions(languageCode).stream()
                .map(ReligionOptionItemResponse::from)
                .toList();
        return new ReligionOptionsResponse(religions);
    }

    @Transactional(readOnly = true)
    public CountryOptionsResponse getCountryOptions(Long userId) {
        String languageCode = userPreferenceService.getLanguage(userId);
        Locale locale = toLocale(languageCode);
        List<CountryOptionItemResponse> countries = getCountries().stream()
                .map(option -> new CountryOptionItemResponse(
                        option.code(),
                        resolveLocalizedCountryName(option.code(), option.name(), locale)
                ))
                .toList();
        return new CountryOptionsResponse(countries);
    }

    @Transactional(readOnly = true)
    public SchoolOptionsResponse getSchoolOptions(Long userId) {
        String languageCode = userPreferenceService.getLanguage(userId);
        List<SchoolOptionItemResponse> schools = getSchools(languageCode).stream()
                .map(SchoolOptionItemResponse::from)
                .toList();
        return new SchoolOptionsResponse(schools);
    }

    @Transactional(readOnly = true)
    public List<LanguageOption> getLanguages() {
        return settingsMasterQueryPort.findLanguages();
    }

    @Transactional(readOnly = true)
    public AllergyOptionsResponse getAllergyOptions(Long userId) {
        String languageCode = userPreferenceService.getLanguage(userId);
        List<AllergyOptionItemResponse> allergies = getAllergies(languageCode).stream()
                .map(AllergyOptionItemResponse::from)
                .toList();
        return new AllergyOptionsResponse(allergies);
    }

    @Transactional(readOnly = true)
    private List<AllergyOption> getAllergies(String langCode) {
        return settingsMasterQueryPort.findAllergyOptions(normalize(langCode));
    }

    @Transactional(readOnly = true)
    public List<ReligiousRestrictionOption> getReligions(String langCode) {
        return settingsMasterQueryPort.findReligiousRestrictions(normalize(langCode));
    }

    @Transactional(readOnly = true)
    public List<CountryOption> getCountries() {
        return settingsMasterQueryPort.findCountries();
    }

    @Transactional(readOnly = true)
    public List<SchoolOption> getSchools(String langCode) {
        return settingsMasterQueryPort.findSchools(normalize(langCode));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Locale toLocale(String languageCode) {
        String normalized = normalize(languageCode);
        if (normalized == null) {
            return Locale.ENGLISH;
        }
        Locale locale = Locale.forLanguageTag(normalized);
        return locale.getLanguage().isBlank() ? Locale.ENGLISH : locale;
    }

    private String resolveLocalizedCountryName(String countryCode, String fallbackName, Locale locale) {
        if (countryCode == null || countryCode.isBlank()) {
            return fallbackName;
        }
        Locale countryLocale = new Locale("", countryCode.trim().toUpperCase(Locale.ROOT));
        String localizedName = countryLocale.getDisplayCountry(locale);
        if (localizedName == null || localizedName.isBlank()) {
            return fallbackName;
        }
        return localizedName;
    }
}

