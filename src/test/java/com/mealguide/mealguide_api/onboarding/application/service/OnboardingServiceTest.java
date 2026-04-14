package com.mealguide.mealguide_api.onboarding.application.service;

import com.mealguide.mealguide_api.onboarding.application.port.SchoolQueryPort;
import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingServiceTest {

    @Test
    void getSchoolsReturnsTranslatedOrFallbackSchoolNamesFromPort() {
        OnboardingService onboardingService = new OnboardingService(new FakeSchoolQueryPort());

        assertThat(onboardingService.getSchools("en"))
                .containsExactly(
                        new SchoolOption(1L, "Kumoh National Institute of Technology"),
                        new SchoolOption(2L, "Base School Name")
                );
    }

    private static class FakeSchoolQueryPort implements SchoolQueryPort {
        @Override
        public List<SchoolOption> findSchools(String langCode) {
            return List.of(
                    new SchoolOption(1L, "Kumoh National Institute of Technology"),
                    new SchoolOption(2L, "Base School Name")
            );
        }
    }
}
