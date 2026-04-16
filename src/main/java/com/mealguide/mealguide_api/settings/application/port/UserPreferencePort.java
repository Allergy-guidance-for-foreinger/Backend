package com.mealguide.mealguide_api.settings.application.port;

import com.mealguide.mealguide_api.settings.domain.UserPreference;

import java.util.List;
import java.util.Optional;

public interface UserPreferencePort {
    Optional<UserPreference> findActiveUserById(Long userId);

    List<String> findAllergyCodesByUserId(Long userId);

    void replaceAllergies(Long userId, List<String> allergyCodes);
}

