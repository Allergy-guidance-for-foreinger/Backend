package com.mealguide.mealguide_api.global.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class UserEmailCryptoService {

    private static final byte PAYLOAD_VERSION = 1;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKey encryptionKey;
    private final SecretKey hmacKey;
    private final String keyId;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserEmailCryptoService(
            @Value("${mealguide.user-email.encryption-key}") String encryptionKey,
            @Value("${mealguide.user-email.hash-key}") String hashKey,
            @Value("${mealguide.user-email.key-id:v1}") String keyId
    ) {
        this.encryptionKey = new SecretKeySpec(decodeKey(encryptionKey, 32, "email encryption key"), "AES");
        this.hmacKey = new SecretKeySpec(decodeKey(hashKey, 32, "email hash key"), HMAC_ALGORITHM);
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("email key id must not be blank");
        }
        byte[] keyIdBytes = keyId.getBytes(StandardCharsets.UTF_8);
        if (keyIdBytes.length > 255) {
            throw new IllegalArgumentException("email key id is too long");
        }
        this.keyId = keyId;
    }

    public String encryptEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertextAndTag = cipher.doFinal(normalizeEmail(email).getBytes(StandardCharsets.UTF_8));

            byte[] keyIdBytes = keyId.getBytes(StandardCharsets.UTF_8);
            ByteBuffer payload = ByteBuffer.allocate(1 + 1 + keyIdBytes.length + 1 + iv.length + ciphertextAndTag.length);
            payload.put(PAYLOAD_VERSION);
            payload.put((byte) keyIdBytes.length);
            payload.put(keyIdBytes);
            payload.put((byte) iv.length);
            payload.put(iv);
            payload.put(ciphertextAndTag);
            return Base64.getEncoder().encodeToString(payload.array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("email encryption failed", exception);
        }
    }

    public String decryptEmail(String encryptedEmail) {
        if (encryptedEmail == null || encryptedEmail.isBlank()) {
            return null;
        }

        try {
            ByteBuffer payload = ByteBuffer.wrap(Base64.getDecoder().decode(encryptedEmail));
            byte version = payload.get();
            if (version != PAYLOAD_VERSION) {
                throw new IllegalArgumentException("unsupported email encryption payload version");
            }

            int keyIdLength = Byte.toUnsignedInt(payload.get());
            byte[] storedKeyIdBytes = new byte[keyIdLength];
            payload.get(storedKeyIdBytes);
            String storedKeyId = new String(storedKeyIdBytes, StandardCharsets.UTF_8);
            if (!keyId.equals(storedKeyId)) {
                throw new IllegalArgumentException("email encryption key id does not match configured key id");
            }

            int ivLength = Byte.toUnsignedInt(payload.get());
            byte[] iv = new byte[ivLength];
            payload.get(iv);
            byte[] ciphertextAndTag = new byte[payload.remaining()];
            payload.get(ciphertextAndTag);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertextAndTag), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("email decryption failed", exception);
        }
    }

    public String hashEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            byte[] digest = mac.doFinal(normalizeEmail(email).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("email hashing failed", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] decodeKey(String encodedKey, int expectedLength, String keyName) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalArgumentException(keyName + " must not be blank");
        }
        byte[] decoded = Base64.getDecoder().decode(encodedKey);
        if (decoded.length != expectedLength) {
            throw new IllegalArgumentException(keyName + " must be " + expectedLength + " bytes after Base64 decoding");
        }
        return decoded;
    }
}
