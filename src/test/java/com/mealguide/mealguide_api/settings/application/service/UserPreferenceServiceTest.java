package com.mealguide.mealguide_api.settings.application.service;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.settings.application.port.SettingsMasterQueryPort;
import com.mealguide.mealguide_api.settings.application.port.UserPreferencePort;
import com.mealguide.mealguide_api.settings.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPreferenceServiceTest {
    private FakeUserPreferencePort userPreferencePort;
    private UserPreferenceService userPreferenceService;

    @BeforeEach
    void setUp() {
        userPreferencePort = new FakeUserPreferencePort();
        userPreferenceService = new UserPreferenceService(userPreferencePort, new FakeSettingsMasterQueryPort());
        userPreferencePort.user = createUser(1L);
    }

    @Test
    void getReligionReturnsCurrentUserReligions() {
        userPreferencePort.savedReligiousCodes = List.of("HALAL", "VEGAN");
        assertThat(userPreferenceService.getReligion(1L)).containsExactly("HALAL", "VEGAN");
    }

    @Test
    void updateReligionReplacesReligionsWithDeduplication() {
        List<String> updated = userPreferenceService.updateReligion(1L, List.of("HALAL", "VEGAN", "HALAL"));
        assertThat(updated).containsExactly("HALAL", "VEGAN");
        assertThat(userPreferencePort.savedReligiousCodes).containsExactly("HALAL", "VEGAN");
    }

    @Test
    void updateReligionFailsWhenCodeDoesNotExist() {
        assertThatThrownBy(() -> userPreferenceService.updateReligion(1L, List.of("HALAL", "missing")))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_RELIGIOUS_CODE);
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
        private List<String> savedReligiousCodes = List.of();
        @Override public Optional<UserPreference> findActiveUserById(Long userId) { return Optional.ofNullable(user); }
        @Override public List<String> findAllergyCodesByUserId(Long userId) { return savedAllergyCodes; }
        @Override public void replaceAllergies(Long userId, List<String> allergyCodes) { savedAllergyCodes = new ArrayList<>(allergyCodes); }
        @Override public List<String> findReligiousCodesByUserId(Long userId) { return savedReligiousCodes; }
        @Override public void replaceReligiousCodes(Long userId, List<String> religiousCodes) { savedReligiousCodes = new ArrayList<>(religiousCodes); }
    }

    private static class FakeSettingsMasterQueryPort implements SettingsMasterQueryPort {
        private final Set<String> languageCodes = Set.of("ko", "en");
        private final Set<String> allergyCodes = Set.of("EGG", "MILK", "SHRIMP", "CELERY");
        private final Set<String> religiousCodes = Set.of("HALAL", "VEGAN", "HINDU");
        private final Set<String> countryCodes = Set.of("KR", "US");
        private final Set<Long> schoolIds = Set.of(1L, 2L, 10L);
        @Override public List<LanguageOption> findLanguages() { return List.of(); }
        @Override public boolean existsLanguageCode(String languageCode) { return languageCodes.contains(languageCode); }
        @Override public List<AllergyOption> findAllergyOptions(String langCode) { return List.of(); }
        @Override public boolean existsAllAllergyCodes(Set<String> allergyCodes) { return this.allergyCodes.containsAll(allergyCodes); }
        @Override public List<ReligiousRestrictionOption> findReligiousRestrictions(String langCode) { return List.of(); }
        @Override public boolean existsReligiousCode(String religiousCode) { return religiousCodes.contains(religiousCode); }
        @Override public boolean existsAllReligiousCodes(Set<String> religiousCodes) { return this.religiousCodes.containsAll(religiousCodes); }
        @Override public List<CountryOption> findCountries() { return List.of(); }
        @Override public boolean existsCountryCode(String countryCode) { return countryCodes.contains(countryCode); }
        @Override public List<SchoolOption> findSchools(String langCode) { return List.of(); }
        @Override public boolean existsSchoolId(Long schoolId) { return schoolIds.contains(schoolId); }
    }
}
