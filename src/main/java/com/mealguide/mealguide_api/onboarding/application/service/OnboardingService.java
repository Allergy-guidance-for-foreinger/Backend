package com.mealguide.mealguide_api.onboarding.application.service;

import com.mealguide.mealguide_api.onboarding.application.port.SchoolQueryPort;
import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final SchoolQueryPort schoolQueryPort;

    @Transactional(readOnly = true)
    public List<SchoolOption> getSchools(String langCode) {
        return schoolQueryPort.findSchools(normalize(langCode));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
