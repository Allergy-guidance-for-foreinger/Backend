package com.mealguide.mealguide_api.settings.application.service;

import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.application.port.UserPreferencePort;
import com.mealguide.mealguide_api.settings.domain.AllergyOption;
import com.mealguide.mealguide_api.settings.domain.CountryOption;
import com.mealguide.mealguide_api.settings.domain.LanguageOption;
import com.mealguide.mealguide_api.settings.domain.ReligiousRestrictionOption;
import com.mealguide.mealguide_api.settings.domain.SchoolOption;
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
    void replaceAllergiesAllowsAdditionalAllergyCode() {
        List<String> updated = userPreferenceService.replaceAllergies(1L, List.of("CELERY"));

        assertThat(updated).containsExactly("CELERY");
        assertThat(userPreferencePort.savedAllergyCodes).containsExactly("CELERY");
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

    @Test
    void getCountryReturnsCurrentUserCountry() {
        userPreferencePort.user.updateCountryCode("KR");

        assertThat(userPreferenceService.getCountry(1L)).isEqualTo("KR");
    }

    @Test
    void updateCountrySuccess() {
        String updated = userPreferenceService.updateCountry(1L, "KR");

        assertThat(updated).isEqualTo("KR");
        assertThat(userPreferencePort.user.getCountryCode()).isEqualTo("KR");
    }

    @Test
    void updateCountryFailsWhenCodeDoesNotExist() {
        assertThatThrownBy(() -> userPreferenceService.updateCountry(1L, "missing"))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUNTRY_CODE);
    }

    @Test
    void getSchoolReturnsCurrentUserSchool() {
        userPreferencePort.user.updateSchoolId(1L);

        assertThat(userPreferenceService.getSchool(1L)).isEqualTo(1L);
    }

    @Test
    void updateSchoolSuccess() {
        Long updated = userPreferenceService.updateSchool(1L, 2L);

        assertThat(updated).isEqualTo(2L);
        assertThat(userPreferencePort.user.getSchoolId()).isEqualTo(2L);
    }

    @Test
    void updateSchoolFailsWhenSchoolIdDoesNotExist() {
        assertThatThrownBy(() -> userPreferenceService.updateSchool(1L, 999L))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SCHOOL_ID);
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
        private final Set<String> allergyCodes = Set.of("EGG", "MILK", "SHRIMP", "CELERY");
        private final Set<String> religiousCodes = Set.of("HALAL", "HINDU");
        private final Set<String> countryCodes = Set.of("KR", "US");
        private final Set<Long> schoolIds = Set.of(1L, 2L, 10L);

        @Override
        public List<LanguageOption> findLanguages() {
            return List.of();
        }

        @Override
        public boolean existsLanguageCode(String languageCode) {
            return languageCodes.contains(languageCode);
        }

        @Override
        public List<AllergyOption> findPrimaryAllergies(String langCode) {
            return List.of();
        }

        @Override
        public List<AllergyOption> findAdditionalAllergies(String langCode) {
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

        @Override
        public List<CountryOption> findCountries() {
            return List.of();
        }

        @Override
        public boolean existsCountryCode(String countryCode) {
            return countryCodes.contains(countryCode);
        }

        @Override
        public List<SchoolOption> findSchools(String langCode) {
            return List.of();
        }

        @Override
        public boolean existsSchoolId(Long schoolId) {
            return schoolIds.contains(schoolId);
        }
    }
}

