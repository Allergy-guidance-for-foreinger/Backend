package com.mealguide.mealguide_api.settings.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.AllergyJpaRepository;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.LanguageJpaRepository;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.ReligiousFoodRestrictionJpaRepository;
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
    public List<AllergyOption> findAllergies(String langCode) {
        return allergyJpaRepository.findAllergyOptions(langCode);
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
}
