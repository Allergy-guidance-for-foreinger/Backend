package com.mealguide.mealguide_api.settings.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.settings.application.port.UserPreferencePort;
import com.mealguide.mealguide_api.settings.domain.UserAllergy;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.UserAllergyJpaRepository;
import com.mealguide.mealguide_api.settings.infrastructure.persistence.repository.UserPreferenceJpaRepository;
import com.mealguide.mealguide_api.settings.domain.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPreferencePersistenceAdapter implements UserPreferencePort {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final UserPreferenceJpaRepository userPreferenceJpaRepository;
    private final UserAllergyJpaRepository userAllergyJpaRepository;

    @Override
    public Optional<UserPreference> findActiveUserById(Long userId) {
        return userPreferenceJpaRepository.findByIdAndDeletedAtIsNullAndStatus(userId, ACTIVE_STATUS);
    }

    @Override
    public List<String> findAllergyCodesByUserId(Long userId) {
        return userAllergyJpaRepository.findAllergyCodesByUserIdOrderByDisplayOrder(userId);
    }

    @Override
    public void replaceAllergies(Long userId, List<String> allergyCodes) {
        userAllergyJpaRepository.deleteByUserId(userId);
        List<UserAllergy> userAllergies = allergyCodes.stream()
                .map(allergyCode -> UserAllergy.create(userId, allergyCode))
                .toList();
        userAllergyJpaRepository.saveAll(userAllergies);
    }
}
