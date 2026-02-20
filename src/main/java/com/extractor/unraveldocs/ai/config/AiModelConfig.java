package com.extractor.unraveldocs.ai.config;

import com.extractor.unraveldocs.ai.provider.AiModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI model selection and fallback logic.
 * Provides the appropriate ChatModel based on the requested provider,
 * with automatic fallback to a secondary provider if the primary is
 * unavailable.
 */
@Slf4j
@Configuration
public class AiModelConfig {

    private final OpenAiChatModel openAiChatModel;
    private final MistralAiChatModel mistralAiChatModel;
    private final AiProperties aiProperties;

    public AiModelConfig(
            OpenAiChatModel openAiChatModel,
            MistralAiChatModel mistralAiChatModel,
            AiProperties aiProperties) {
        this.openAiChatModel = openAiChatModel;
        this.mistralAiChatModel = mistralAiChatModel;
        this.aiProperties = aiProperties;
    }

    /**
     * Get the ChatModel for the specified provider.
     *
     * @param provider The desired AI model provider
     * @return The corresponding ChatModel
     */
    public ChatModel getModel(AiModelProvider provider) {
        return switch (provider) {
            case OPENAI -> openAiChatModel;
            case MISTRAL_AI -> mistralAiChatModel;
        };
    }

    /**
     * Get the default AI model provider.
     *
     * @return The default provider
     */
    public AiModelProvider getDefaultProvider() {
        return aiProperties.getDefaultProvider();
    }

    /**
     * Get the fallback AI model provider.
     *
     * @return The fallback provider
     */
    public AiModelProvider getFallbackProvider() {
        return aiProperties.getFallbackProvider();
    }

    /**
     * Check whether fallback is enabled.
     *
     * @return true if fallback is enabled
     */
    public boolean isFallbackEnabled() {
        return aiProperties.isFallbackEnabled();
    }
}
