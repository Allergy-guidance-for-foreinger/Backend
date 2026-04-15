package com.mealguide.mealguide_api.onboarding.application.service;

import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.onboarding.application.port.OnboardingCommandPort;
import com.mealguide.mealguide_api.onboarding.application.port.SchoolQueryPort;
import com.mealguide.mealguide_api.onboarding.domain.OnboardingCompletion;
import com.mealguide.mealguide_api.onboarding.domain.SchoolOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnboardingServiceTest {

    @Test
    void getSchoolsReturnsTranslatedOrFallbackSchoolNamesFromPort() {
        OnboardingService onboardingService = new OnboardingService(new FakeSchoolQueryPort(), new FakeOnboardingCommandPort());

        assertThat(onboardingService.getSchools("en"))
                .containsExactly(
                        new SchoolOption(1L, "Kumoh National Institute of Technology"),
                        new SchoolOption(2L, "Base School Name")
                );
    }

    @Test
    void completeOnboardingSavesSchoolAllergiesReligionAndCompletionFlag() {
        FakeOnboardingCommandPort commandPort = new FakeOnboardingCommandPort();
        OnboardingService onboardingService = new OnboardingService(new FakeSchoolQueryPort(), commandPort);

        OnboardingCompletion completion = onboardingService.completeOnboarding(
                1L,
                "en",
                10L,
                List.of("EGG", "MILK", "EGG"),
                "HALAL"
        );

        assertThat(completion.languageCode()).isEqualTo("en");
        assertThat(completion.schoolId()).isEqualTo(10L);
        assertThat(completion.allergyCodes()).containsExactly("EGG", "MILK");
        assertThat(completion.religiousCode()).isEqualTo("HALAL");
        assertThat(completion.onboardingCompleted()).isTrue();
    }

    @Test
    void completeOnboardingFailsWhenSchoolIdIsInvalid() {
        OnboardingService onboardingService = new OnboardingService(new FakeSchoolQueryPort(), new FakeOnboardingCommandPort());

        assertThatThrownBy(() -> onboardingService.completeOnboarding(
                1L,
                "en",
                999L,
                List.of("EGG"),
                null
        )).isInstanceOf(ServiceException.class);
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

    private static class FakeOnboardingCommandPort implements OnboardingCommandPort {
        @Override
        public boolean existsActiveUserById(Long userId) {
            return userId == 1L;
        }

        @Override
        public boolean existsSchoolById(Long schoolId) {
            return schoolId == 10L;
        }

        @Override
        public boolean existsAllAllergyCodes(Set<String> allergyCodes) {
            return allergyCodes.stream().allMatch(code -> code.equals("EGG") || code.equals("MILK"));
        }

        @Override
        public boolean existsLanguageCode(String languageCode) {
            return "en".equals(languageCode) || "ko".equals(languageCode);
        }

        @Override
        public boolean existsReligiousCode(String religiousCode) {
            return "HALAL".equals(religiousCode);
        }

        @Override
        public void replaceAllergies(Long userId, List<String> allergyCodes) {
            // no-op for test fake
        }

        @Override
        public boolean completeOnboarding(Long userId, String languageCode, Long schoolId, String religiousCode) {
            // no-op for test fake
            return true;
        }
    }
}
