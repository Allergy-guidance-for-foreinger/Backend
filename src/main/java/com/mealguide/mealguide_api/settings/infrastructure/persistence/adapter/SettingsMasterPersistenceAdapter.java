package com.mealguide.mealguide_api.settings.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.domain.AllergyGroup;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.CountryOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import com.mealguide.mealguide_api.settings.domain.SchoolOption;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.AllergyJpaRepository;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.CountryJpaRepository;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.LanguageJpaRepository;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.ReligiousFoodRestrictionJpaRepository;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.SettingsSchoolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SettingsMasterPersistenceAdapter implements SettingsMasterQueryPort {

    private final LanguageJpaRepository languageJpaRepository;
    private final AllergyJpaRepository allergyJpaRepository;
    private final ReligiousFoodRestrictionJpaRepository religiousFoodRestrictionJpaRepository;
    private final CountryJpaRepository countryJpaRepository;
    private final SettingsSchoolJpaRepository schoolJpaRepository;

    @Override
    public List<LanguageOption> findLanguages() {
        return languageJpaRepository.findAllByOrderByCodeAsc().stream()
                .map(language -> new LanguageOption(language.getCode(), language.getName(), language.getEnglishName()))
                .toList();
    }

    @Override
    public boolean existsLanguageCode(String languageCode) {
        return languageJpaRepository.existsByCode(languageCode);
    }

    @Override
    public List<AllergyOption> findPrimaryAllergies(String langCode) {
        return allergyJpaRepository.findAllergyOptionsByGroup(langCode, AllergyGroup.PRIMARY);
    }

    @Override
    public List<AllergyOption> findAdditionalAllergies(String langCode) {
        return allergyJpaRepository.findAllergyOptionsByGroup(langCode, AllergyGroup.ADDITIONAL);
    }

    @Override
    public boolean existsAllAllergyCodes(Set<String> allergyCodes) {
        if (allergyCodes.isEmpty()) {
            return true;
        }
        return allergyJpaRepository.countByCodeIn(allergyCodes) == allergyCodes.size();
    }

    @Override
    public List<ReligiousRestrictionOption> findReligiousRestrictions(String langCode) {
        return religiousFoodRestrictionJpaRepository.findReligiousRestrictionOptions(langCode);
    }

    @Override
    public boolean existsReligiousCode(String religiousCode) {
        return religiousFoodRestrictionJpaRepository.existsByCode(religiousCode);
    }

    @Override
    public List<CountryOption> findCountries() {
        return countryJpaRepository.findAllByOrderByNameAsc().stream()
                .map(country -> new CountryOption(country.getCode(), country.getName()))
                .toList();
    }

    @Override
    public boolean existsCountryCode(String countryCode) {
        return countryJpaRepository.existsByCode(countryCode);
    }

    @Override
    public List<SchoolOption> findSchools(String langCode) {
        return schoolJpaRepository.findSchoolOptions(langCode);
    }

    @Override
    public boolean existsSchoolId(Long schoolId) {
        return schoolJpaRepository.existsById(schoolId);
    }
}

