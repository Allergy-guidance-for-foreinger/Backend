package com.mealguide.mealguide_api.global.security.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserEmailCryptoServiceTest {

    private final UserEmailCryptoService userEmailCryptoService = new UserEmailCryptoService(
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
            "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=",
            "test-v1"
    );

    @Test
    void encryptEmailCanBeDecrypted() {
        String encrypted = userEmailCryptoService.encryptEmail("User@Test.COM ");

        assertThat(encrypted).isNotBlank();
        assertThat(userEmailCryptoService.decryptEmail(encrypted)).isEqualTo("user@test.com");
    }

    @Test
    void encryptEmailUsesFreshIv() {
        String first = userEmailCryptoService.encryptEmail("user@test.com");
        String second = userEmailCryptoService.encryptEmail("user@test.com");

        assertThat(first).isNotEqualTo(second);
        assertThat(userEmailCryptoService.decryptEmail(first)).isEqualTo("user@test.com");
        assertThat(userEmailCryptoService.decryptEmail(second)).isEqualTo("user@test.com");
    }

    @Test
    void hashEmailIsStableForNormalizedEmail() {
        String first = userEmailCryptoService.hashEmail("User@Test.COM ");
        String second = userEmailCryptoService.hashEmail(" user@test.com");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void decryptEmailWrapsMalformedPayload() {
        assertThatThrownBy(() -> userEmailCryptoService.decryptEmail("not-base64"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email decryption failed");
    }

    @Test
    void decryptEmailWrapsTruncatedPayload() {
        assertThatThrownBy(() -> userEmailCryptoService.decryptEmail("AQ=="))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email decryption failed");
    }
}
