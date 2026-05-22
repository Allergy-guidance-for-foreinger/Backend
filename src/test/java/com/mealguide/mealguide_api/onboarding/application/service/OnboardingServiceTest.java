package com.mealguide.mealguide_api.onboarding.application.service;

import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.onboarding.application.port.OnboardingCommandPort;
import com.mealguide.mealguide_api.onboarding.domain.OnboardingCompletion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnboardingServiceTest {

    @Test
    void completeOnboardingSavesSchoolAllergiesReligionsAndCompletionFlag() {
        FakeOnboardingCommandPort commandPort = new FakeOnboardingCommandPort();
        OnboardingService onboardingService = new OnboardingService(commandPort);

        OnboardingCompletion completion = onboardingService.completeOnboarding(
                1L, "en", 10L, List.of("EGG", "MILK", "EGG"), List.of("HALAL", "HALAL"), "KR"
        );

        assertThat(completion.languageCode()).isEqualTo("en");
        assertThat(completion.schoolId()).isEqualTo(10L);
        assertThat(completion.allergyCodes()).containsExactly("EGG", "MILK");
        assertThat(completion.religiousCodes()).containsExactly("HALAL");
        assertThat(completion.countryCode()).isEqualTo("KR");
        assertThat(completion.onboardingCompleted()).isTrue();
    }

    @Test
    void completeOnboardingAllowsEmptyReligiousCodes() {
        OnboardingService onboardingService = new OnboardingService(new FakeOnboardingCommandPort());
        OnboardingCompletion completion = onboardingService.completeOnboarding(
                1L, "en", 10L, List.of("CELERY"), List.of(), "KR"
        );
        assertThat(completion.religiousCodes()).isEmpty();
    }

    @Test
    void completeOnboardingFailsWhenSchoolIdIsInvalid() {
        OnboardingService onboardingService = new OnboardingService(new FakeOnboardingCommandPort());
        assertThatThrownBy(() -> onboardingService.completeOnboarding(
                1L, "en", 999L, List.of("EGG"), List.of(), "KR"
        )).isInstanceOf(ServiceException.class);
    }

    private static class FakeOnboardingCommandPort implements OnboardingCommandPort {
        @Override public boolean existsActiveUserById(Long userId) { return userId == 1L; }
        @Override public boolean existsSchoolById(Long schoolId) { return schoolId == 10L; }
        @Override public boolean existsLanguageCode(String languageCode) { return "en".equals(languageCode) || "ko".equals(languageCode); }
        @Override public boolean existsAllAllergyCodes(Set<String> allergyCodes) { return allergyCodes.stream().allMatch(c -> Set.of("EGG", "MILK", "CELERY").contains(c)); }
        @Override public boolean existsAllReligiousCodes(Set<String> religiousCodes) { return Set.of("HALAL", "VEGAN").containsAll(religiousCodes); }
        @Override public boolean existsCountryCode(String countryCode) { return "KR".equals(countryCode) || "US".equals(countryCode); }
        @Override public void replaceAllergies(Long userId, List<String> allergyCodes) { }
        @Override public void replaceReligiousRestrictions(Long userId, List<String> religiousCodes) { }
        @Override public boolean completeOnboarding(Long userId, String languageCode, Long schoolId, String countryCode) { return true; }
    }
}
