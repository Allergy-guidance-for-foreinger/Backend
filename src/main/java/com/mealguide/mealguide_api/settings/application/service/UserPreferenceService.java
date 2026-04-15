package com.mealguide.mealguide_api.settings.application.service;

import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.application.port.UserPreferencePort;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.settings.domain.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferencePort userPreferencePort;
    private final SettingsMasterQueryPort settingsMasterQueryPort;

    @Transactional(readOnly = true)
    public String getLanguage(Long userId) {
        return findUser(userId).getLanguageCode();
    }

    @Transactional(readOnly = true)
    public List<String> getAllergies(Long userId) {
        findUser(userId);
        return userPreferencePort.findAllergyCodesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public String getReligion(Long userId) {
        return findUser(userId).getReligiousCode();
    }

    @Transactional
    public String updateLanguage(Long userId, String languageCode) {
        String normalizedLanguageCode = requireText(languageCode, ErrorCode.INVALID_LANGUAGE_CODE);
        if (!settingsMasterQueryPort.existsLanguageCode(normalizedLanguageCode)) {
            throw new ServiceException(ErrorCode.INVALID_LANGUAGE_CODE);
        }

        UserPreference user = findUser(userId);
        user.updateLanguageCode(normalizedLanguageCode);
        return user.getLanguageCode();
    }

    @Transactional
    public List<String> replaceAllergies(Long userId, List<String> allergyCodes) {
        List<String> normalizedAllergyCodes = normalizeAllergyCodes(allergyCodes);
        if (!settingsMasterQueryPort.existsAllAllergyCodes(Set.copyOf(normalizedAllergyCodes))) {
            throw new ServiceException(ErrorCode.INVALID_ALLERGY_CODE);
        }

        findUser(userId);
        userPreferencePort.replaceAllergies(userId, normalizedAllergyCodes);
        return normalizedAllergyCodes;
    }

    @Transactional
    public String updateReligion(Long userId, String religiousCode) {
        String normalizedReligiousCode = normalize(religiousCode);
        if (normalizedReligiousCode != null && !settingsMasterQueryPort.existsReligiousCode(normalizedReligiousCode)) {
            throw new ServiceException(ErrorCode.INVALID_RELIGIOUS_CODE);
        }

        UserPreference user = findUser(userId);
        user.updateReligiousCode(normalizedReligiousCode);
        return user.getReligiousCode();
    }

    private UserPreference findUser(Long userId) {
        return userPreferencePort.findActiveUserById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
    }

    private List<String> normalizeAllergyCodes(List<String> allergyCodes) {
        if (allergyCodes == null) {
            throw new ServiceException(ErrorCode.INVALID_ALLERGY_CODE);
        }

        Set<String> deduplicated = new LinkedHashSet<>();
        for (String allergyCode : allergyCodes) {
            deduplicated.add(requireText(allergyCode, ErrorCode.INVALID_ALLERGY_CODE));
        }
        return new ArrayList<>(deduplicated);
    }

    private String requireText(String value, ErrorCode errorCode) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ServiceException(errorCode);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
