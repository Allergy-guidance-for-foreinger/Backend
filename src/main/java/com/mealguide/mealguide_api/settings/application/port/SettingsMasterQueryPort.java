package com.mealguide.mealguide_api.settings.application.port;

import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;

import java.util.List;
import java.util.Set;

public interface SettingsMasterQueryPort {
    List<LanguageOption> findLanguages();

    boolean existsLanguageCode(String languageCode);

    List<AllergyOption> findAllergies(String langCode);

    boolean existsAllAllergyCodes(Set<String> allergyCodes);

    List<ReligiousRestrictionOption> findReligiousRestrictions(String langCode);

    boolean existsReligiousCode(String religiousCode);
}
