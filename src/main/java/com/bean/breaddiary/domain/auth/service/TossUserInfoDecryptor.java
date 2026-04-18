package com.bean.breaddiary.domain.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

@Service
public class TossUserInfoDecryptor {

    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final String decryptionKey;
    private final String additionalAuthenticatedData;

    public TossUserInfoDecryptor(
            @Value("${app.auth.toss.decryption-key:}") String decryptionKey,
            @Value("${app.auth.toss.aad:}") String additionalAuthenticatedData
    ) {
        this.decryptionKey = decryptionKey;
        this.additionalAuthenticatedData = additionalAuthenticatedData;
    }

    public String decryptNullable(String encryptedValue) {
        if (!StringUtils.hasText(encryptedValue)) {
            return null;
        }

        if (!isConfigured()) {
            return null;
        }

        try {
            byte[] decodedCipherText = Base64.getDecoder().decode(encryptedValue);
            byte[] iv = Arrays.copyOfRange(decodedCipherText, 0, GCM_IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(decodedCipherText, GCM_IV_LENGTH_BYTES, decodedCipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(Base64.getDecoder().decode(decryptionKey), "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            );
            cipher.updateAAD(additionalAuthenticatedData.getBytes(StandardCharsets.UTF_8));

            String decryptedValue = new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8).trim();
            return StringUtils.hasText(decryptedValue) ? decryptedValue : null;
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "토스 사용자 정보 복호화에 실패했습니다.",
                    exception
            );
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(decryptionKey)
                && StringUtils.hasText(additionalAuthenticatedData);
    }
}
