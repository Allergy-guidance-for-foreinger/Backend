package com.mealguide.mealguide_api.settings.application.service;

import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.application.port.UserPreferencePort;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import com.mealguide.mealguide_api.settings.domain.UserPreference;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPreferenceServiceTest {

    private FakeUserPreferencePort userPreferencePort;
    private FakeSettingsMasterQueryPort settingsMasterQueryPort;
    private UserPreferenceService userPreferenceService;

    @BeforeEach
    void setUp() {
        userPreferencePort = new FakeUserPreferencePort();
        settingsMasterQueryPort = new FakeSettingsMasterQueryPort();
        userPreferenceService = new UserPreferenceService(userPreferencePort, settingsMasterQueryPort);
        userPreferencePort.user = createUser(1L);
    }

    @Test
    void getLanguageReturnsCurrentUserLanguage() {
        userPreferencePort.user.updateLanguageCode("en");

        assertThat(userPreferenceService.getLanguage(1L)).isEqualTo("en");
    }

    @Test
    void getAllergiesReturnsCurrentUserAllergies() {
        userPreferencePort.savedAllergyCodes = List.of("EGG", "MILK");

        assertThat(userPreferenceService.getAllergies(1L)).containsExactly("EGG", "MILK");
    }

    @Test
    void getReligionReturnsCurrentUserReligion() {
        userPreferencePort.user.updateReligiousCode("HALAL");

        assertThat(userPreferenceService.getReligion(1L)).isEqualTo("HALAL");
    }

    @Test
    void updateLanguageSuccess() {
        String updated = userPreferenceService.updateLanguage(1L, "en");

        assertThat(updated).isEqualTo("en");
        assertThat(userPreferencePort.user.getLanguageCode()).isEqualTo("en");
    }

    @Test
    void updateLanguageFailsWhenLanguageCodeDoesNotExist() {
        assertThatThrownBy(() -> userPreferenceService.updateLanguage(1L, "missing"))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_LANGUAGE_CODE);
    }

    @Test
    void replaceAllergiesSuccessDeduplicatesRequest() {
        List<String> updated = userPreferenceService.replaceAllergies(1L, List.of("EGG", "MILK", "EGG"));

        assertThat(updated).containsExactly("EGG", "MILK");
        assertThat(userPreferencePort.savedAllergyCodes).containsExactly("EGG", "MILK");
    }

    @Test
    void replaceAllergiesFailsWhenAnyCodeDoesNotExist() {
        assertThatThrownBy(() -> userPreferenceService.replaceAllergies(1L, List.of("EGG", "missing")))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ALLERGY_CODE);
    }

    @Test
    void updateReligionSuccess() {
        String updated = userPreferenceService.updateReligion(1L, "HALAL");

        assertThat(updated).isEqualTo("HALAL");
        assertThat(userPreferencePort.user.getReligiousCode()).isEqualTo("HALAL");
    }

    @Test
    void updateReligionFailsWhenCodeDoesNotExist() {
        assertThatThrownBy(() -> userPreferenceService.updateReligion(1L, "missing"))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_RELIGIOUS_CODE);
    }

    @Test
    void updateReligionSupportsClearingWithNull() {
        userPreferencePort.user.updateReligiousCode("HALAL");

        String updated = userPreferenceService.updateReligion(1L, null);

        assertThat(updated).isNull();
        assertThat(userPreferencePort.user.getReligiousCode()).isNull();
    }

    private UserPreference createUser(Long id) {
        UserPreference user = BeanUtils.instantiateClass(UserPreference.class);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "status", "ACTIVE");
        return user;
    }

    private static class FakeUserPreferencePort implements UserPreferencePort {
        private UserPreference user;
        private List<String> savedAllergyCodes = List.of();

        @Override
        public Optional<UserPreference> findActiveUserById(Long userId) {
            return Optional.ofNullable(user);
        }

        @Override
        public List<String> findAllergyCodesByUserId(Long userId) {
            return savedAllergyCodes;
        }

        @Override
        public void replaceAllergies(Long userId, List<String> allergyCodes) {
            savedAllergyCodes = new ArrayList<>(allergyCodes);
        }
    }

    private static class FakeSettingsMasterQueryPort implements SettingsMasterQueryPort {
        private final Set<String> languageCodes = Set.of("ko", "en");
        private final Set<String> allergyCodes = Set.of("EGG", "MILK", "SHRIMP");
        private final Set<String> religiousCodes = Set.of("HALAL", "HINDU");

        @Override
        public List<LanguageOption> findLanguages() {
            return List.of();
        }

        @Override
        public boolean existsLanguageCode(String languageCode) {
            return languageCodes.contains(languageCode);
        }

        @Override
        public List<AllergyOption> findAllergies(String langCode) {
            return List.of();
        }

        @Override
        public boolean existsAllAllergyCodes(Set<String> allergyCodes) {
            return this.allergyCodes.containsAll(allergyCodes);
        }

        @Override
        public List<ReligiousRestrictionOption> findReligiousRestrictions(String langCode) {
            return List.of();
        }

        @Override
        public boolean existsReligiousCode(String religiousCode) {
            return religiousCodes.contains(religiousCode);
        }
    }
}
