package com.extractor.unraveldocs.ai.provider;

import lombok.Getter;

/**
 * Enum representing the available AI model providers.
 */
@Getter
public enum AiModelProvider {
    OPENAI("openai", "GPT-4o-mini"),
    MISTRAL_AI("mistral", "Mistral Small");

    private final String key;
    private final String displayName;

    AiModelProvider(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    /**
     * Resolve an AiModelProvider from a string key.
     * Falls back to OPENAI if unrecognized.
     */
    public static AiModelProvider fromKey(String key) {
        if (key == null) {
            return OPENAI;
        }
        for (AiModelProvider provider : values()) {
            if (provider.key.equalsIgnoreCase(key)) {
                return provider;
            }
        }
        return OPENAI;
    }
}
