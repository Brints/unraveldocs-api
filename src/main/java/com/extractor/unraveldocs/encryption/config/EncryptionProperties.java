package com.extractor.unraveldocs.encryption.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionProperties {
    /**
     * Whether encryption is enabled.
     */
    private boolean enabled = true;

    /**
     * Master encryption key (32 bytes, base64-encoded).
     * Set via ENCRYPTION_MASTER_KEY environment variable.
     */
    private String masterKey;

    /**
     * Encryption algorithm (default: AES/GCM/NoPadding).
     */
    private String algorithm = "AES/GCM/NoPadding";

    /**
     * GCM initialization vector length in bytes (default: 12).
     */
    private int ivLength = 12;

    /**
     * GCM authentication tag length in bits (default: 128).
     */
    private int tagLength = 128;
}
