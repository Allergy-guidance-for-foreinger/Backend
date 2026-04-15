package com.mealguide.mealguide_api.onboarding.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.onboarding.application.port.OnboardingCommandPort;
import com.mealguide.mealguide_api.onboarding.application.port.SchoolQueryPort;
import com.mealguide.mealguide_api.onboarding.domain.OnboardingCompletion;
import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final SchoolQueryPort schoolQueryPort;
    private final OnboardingCommandPort onboardingCommandPort;

    @Transactional(readOnly = true)
    public List<SchoolOption> getSchools(String langCode) {
        return schoolQueryPort.findSchools(normalize(langCode));
    }

    @Transactional
    public OnboardingCompletion completeOnboarding(
            Long userId,
            String languageCode,
            Long schoolId,
            List<String> allergyCodes,
            String religiousCode
    ) {
        String normalizedLanguageCode = requireText(languageCode, ErrorCode.INVALID_LANGUAGE_CODE);
        Long normalizedSchoolId = requireSchoolId(schoolId);
        List<String> normalizedAllergyCodes = normalizeAllergyCodes(allergyCodes);
        String normalizedReligiousCode = normalize(religiousCode);

        if (!onboardingCommandPort.existsActiveUserById(userId)) {
            throw new ServiceException(ErrorCode.USER_NOT_FOUND);
        }

        if (!onboardingCommandPort.existsSchoolById(normalizedSchoolId)) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }

        if (!onboardingCommandPort.existsLanguageCode(normalizedLanguageCode)) {
            throw new ServiceException(ErrorCode.INVALID_LANGUAGE_CODE);
        }

        if (!onboardingCommandPort.existsAllAllergyCodes(Set.copyOf(normalizedAllergyCodes))) {
            throw new ServiceException(ErrorCode.INVALID_ALLERGY_CODE);
        }

        if (normalizedReligiousCode != null && !onboardingCommandPort.existsReligiousCode(normalizedReligiousCode)) {
            throw new ServiceException(ErrorCode.INVALID_RELIGIOUS_CODE);
        }

        onboardingCommandPort.replaceAllergies(userId, normalizedAllergyCodes);
        boolean updated = onboardingCommandPort.completeOnboarding(userId, normalizedLanguageCode, normalizedSchoolId, normalizedReligiousCode);
        if (!updated) {
            throw new ServiceException(ErrorCode.USER_NOT_FOUND);
        }

        return new OnboardingCompletion(
                normalizedLanguageCode,
                normalizedSchoolId,
                normalizedAllergyCodes,
                normalizedReligiousCode,
                true
        );
    }

    private Long requireSchoolId(Long schoolId) {
        if (schoolId == null || schoolId <= 0) {
            throw new ServiceException(ErrorCode.BINDING_ERROR);
        }
        return schoolId;
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
