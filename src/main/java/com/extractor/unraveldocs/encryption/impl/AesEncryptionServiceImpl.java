package com.extractor.unraveldocs.encryption.impl;

import com.extractor.unraveldocs.encryption.config.EncryptionProperties;
import com.extractor.unraveldocs.encryption.interfaces.EncryptionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption implementation.
 * Provides bank-level 256-bit encryption using Java's built-in crypto
 * libraries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AesEncryptionServiceImpl implements EncryptionService {

    private final EncryptionProperties encryptionProperties;
    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        if (encryptionProperties.isEnabled() && encryptionProperties.getMasterKey() != null
                && !encryptionProperties.getMasterKey().isBlank()) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(encryptionProperties.getMasterKey());
                if (keyBytes.length != 32) {
                    log.error("Master key must be exactly 32 bytes (256 bits). Got {} bytes", keyBytes.length);
                    return;
                }
                this.secretKey = new SecretKeySpec(keyBytes, "AES");
                log.info("Encryption service initialized successfully with AES-256-GCM");
            } catch (IllegalArgumentException e) {
                log.error("Failed to decode master key. Ensure it is valid base64: {}", e.getMessage());
            }
        } else {
            log.warn("Encryption is disabled or master key is not configured. " +
                    "Set ENCRYPTION_MASTER_KEY environment variable to enable encryption.");
        }
    }

    @Override
    public EncryptionResult encrypt(byte[] data) {
        if (!isEncryptionAvailable()) {
            throw new IllegalStateException("Encryption is not available. Check configuration.");
        }

        try {
            // Generate random IV
            byte[] iv = new byte[encryptionProperties.getIvLength()];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(encryptionProperties.getAlgorithm());
            GCMParameterSpec gcmSpec = new GCMParameterSpec(encryptionProperties.getTagLength(), iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt data
            byte[] encryptedData = cipher.doFinal(data);
            String ivBase64 = Base64.getEncoder().encodeToString(iv);

            log.debug("Successfully encrypted {} bytes", data.length);
            return new EncryptionResult(encryptedData, ivBase64);

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] encryptedData, String ivBase64) {
        if (!isEncryptionAvailable()) {
            throw new IllegalStateException("Encryption is not available. Check configuration.");
        }

        try {
            // Decode IV
            byte[] iv = Base64.getDecoder().decode(ivBase64);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(encryptionProperties.getAlgorithm());
            GCMParameterSpec gcmSpec = new GCMParameterSpec(encryptionProperties.getTagLength(), iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt data
            byte[] decryptedData = cipher.doFinal(encryptedData);

            log.debug("Successfully decrypted {} bytes", decryptedData.length);
            return decryptedData;

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    @Override
    public boolean isEncryptionAvailable() {
        return encryptionProperties.isEnabled() && secretKey != null;
    }
}
