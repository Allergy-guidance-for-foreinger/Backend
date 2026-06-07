package com.mealguide.mealguide_api.login.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void createForFirstGoogleLoginMatchesUsersSchemaDefaults() {
        User user = User.createForFirstGoogleLogin("encrypted-email", "hashed-email");

        assertThat(user.getSchoolId()).isNull();
        assertThat(user.getEmailEncrypted()).isEqualTo("encrypted-email");
        assertThat(user.getEmailHash()).isEqualTo("hashed-email");
        assertThat(user.getLanguageCode()).isNull();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.isOnboardingCompleted()).isFalse();
        assertThat(user.getDeletedAt()).isNull();
    }
}

