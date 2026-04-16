package com.mealguide.mealguide_api.login.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void createForFirstGoogleLoginMatchesUsersSchemaDefaults() {
        User user = User.createForFirstGoogleLogin("user@test.com", "Meal Guide");

        assertThat(user.getSchoolId()).isNull();
        assertThat(user.getEmail()).isEqualTo("user@test.com");
        assertThat(user.getName()).isEqualTo("Meal Guide");
        assertThat(user.getLanguageCode()).isNull();
        assertThat(user.getReligiousCode()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.isOnboardingCompleted()).isFalse();
        assertThat(user.getDeletedAt()).isNull();
    }
}

