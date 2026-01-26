package com.extractor.unraveldocs.coupon.helpers;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility for generating unique coupon codes.
 */
@Component
public class CouponCodeGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_CODE_LENGTH = 8;

    /**
     * Generates a unique coupon code with the given prefix.
     * Format: PREFIX-XXXXXXXX (e.g., SAVE20-A3B7C9D1)
     */
    public String generate(String prefix) {
        String randomPart = generateRandomString(DEFAULT_CODE_LENGTH);

        if (prefix != null && !prefix.isBlank()) {
            return prefix.toUpperCase().trim() + "-" + randomPart;
        }
        return randomPart;
    }

    /**
     * Generates a random alphanumeric string.
     */
    public String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Validates a custom coupon code format.
     */
    public boolean isValidCustomCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        // Must be 6-50 characters, alphanumeric with hyphens
        return code.matches("^[A-Za-z0-9-]{6,50}$");
    }
}
