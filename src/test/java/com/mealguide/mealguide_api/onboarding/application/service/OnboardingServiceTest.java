package com.mealguide.mealguide_api.onboarding.application.service;

import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.onboarding.application.port.OnboardingCommandPort;
import com.mealguide.mealguide_api.onboarding.domain.OnboardingCompletion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class OnboardingServiceTest {

    @Test
    void completeOnboardingSavesSchoolAllergiesReligionAndCompletionFlag() {
        FakeOnboardingCommandPort commandPort = new FakeOnboardingCommandPort();
        OnboardingService onboardingService = new OnboardingService(commandPort);

        OnboardingCompletion completion = onboardingService.completeOnboarding(
                1L,
                "en",
                10L,
                List.of("EGG", "MILK", "EGG"),
                "HALAL",
                "KR"
        );

        assertThat(completion.languageCode()).isEqualTo("en");
        assertThat(completion.schoolId()).isEqualTo(10L);
        assertThat(completion.allergyCodes()).containsExactly("EGG", "MILK");
        assertThat(completion.religiousCode()).isEqualTo("HALAL");
        assertThat(completion.countryCode()).isEqualTo("KR");
        assertThat(completion.onboardingCompleted()).isTrue();
    }

    @Test
    void completeOnboardingAllowsAdditionalAllergyCode() {
        FakeOnboardingCommandPort commandPort = new FakeOnboardingCommandPort();
        OnboardingService onboardingService = new OnboardingService(commandPort);

        OnboardingCompletion completion = onboardingService.completeOnboarding(
                1L,
                "en",
                10L,
                List.of("CELERY"),
                null,
                "KR"
        );

        assertThat(completion.allergyCodes()).containsExactly("CELERY");
    }

    @Test
    void completeOnboardingFailsWhenSchoolIdIsInvalid() {
        OnboardingService onboardingService = new OnboardingService(new FakeOnboardingCommandPort());

        assertThatThrownBy(() -> onboardingService.completeOnboarding(
                1L,
                "en",
                999L,
                List.of("EGG"),
                null,
                "KR"
        )).isInstanceOf(ServiceException.class);
    }

    @Test
    void completeOnboardingFailsWhenCountryCodeIsInvalid() {
        OnboardingService onboardingService = new OnboardingService(new FakeOnboardingCommandPort());

        assertThatThrownBy(() -> onboardingService.completeOnboarding(
                1L,
                "en",
                10L,
                List.of("EGG"),
                null,
                "XX"
        )).isInstanceOf(ServiceException.class);
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
            return allergyCodes.stream().allMatch(code -> code.equals("EGG") || code.equals("MILK") || code.equals("CELERY"));
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
        public boolean existsCountryCode(String countryCode) {
            return "KR".equals(countryCode) || "US".equals(countryCode);
        }

        @Override
        public void replaceAllergies(Long userId, List<String> allergyCodes) {
            // no-op for test fake
        }

        @Override
        public boolean completeOnboarding(Long userId, String languageCode, Long schoolId, String religiousCode, String countryCode) {
            // no-op for test fake
            return true;
        }
    }
}

