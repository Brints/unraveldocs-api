package com.extractor.unraveldocs.encryption.interfaces;

/**
 * Service interface for file encryption/decryption operations.
 * Uses AES-256-GCM for bank-level encryption.
 */
public interface EncryptionService {

    /**
     * Encrypts file data using AES-256-GCM.
     *
     * @param data The raw file data to encrypt
     * @return EncryptionResult containing encrypted data and IV
     */
    EncryptionResult encrypt(byte[] data);

    /**
     * Decrypts file data using AES-256-GCM.
     *
     * @param encryptedData The encrypted file data
     * @param iv            The initialization vector used during encryption
     * @return The decrypted raw file data
     */
    byte[] decrypt(byte[] encryptedData, String iv);

    /**
     * Checks if encryption is enabled and properly configured.
     *
     * @return true if encryption is available
     */
    boolean isEncryptionAvailable();

    /**
     * Result object containing encrypted data and the IV used.
     */
    record EncryptionResult(byte[] encryptedData, String iv) {
    }
}
