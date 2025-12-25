package com.extractor.unraveldocs.ocrprocessing.provider;

import com.extractor.unraveldocs.ocrprocessing.config.OcrProperties;
import com.extractor.unraveldocs.ocrprocessing.exception.OcrProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and managing OCR providers.
 * Handles provider selection, fallback logic, and registration.
 */
@Slf4j
@Component
public class OcrProviderFactory {

    private final OcrProperties ocrProperties;
    private final Map<OcrProviderType, OcrProvider> providers = new ConcurrentHashMap<>();

    public OcrProviderFactory(OcrProperties ocrProperties, List<OcrProvider> registeredProviders) {
        this.ocrProperties = ocrProperties;

        // Register all available providers
        for (OcrProvider provider : registeredProviders) {
            registerProvider(provider);
        }

        log.info("OcrProviderFactory initialized with {} providers: {}",
                providers.size(), providers.keySet());
    }

    /**
     * Register an OCR provider.
     *
     * @param provider The provider to register
     */
    public void registerProvider(OcrProvider provider) {
        providers.put(provider.getProviderType(), provider);
        log.debug("Registered OCR provider: {}", provider.getProviderType());
    }

    /**
     * Get a provider by type.
     *
     * @param type The provider type
     * @return The provider instance
     * @throws OcrProcessingException if the provider is not available or not
     *                                enabled
     */
    public OcrProvider getProvider(OcrProviderType type) {
        if (!ocrProperties.isProviderEnabled(type)) {
            throw OcrProcessingException.providerUnavailable(type);
        }

        OcrProvider provider = providers.get(type);
        if (provider == null) {
            throw OcrProcessingException.providerUnavailable(type);
        }

        if (!provider.isAvailable()) {
            throw OcrProcessingException.providerUnavailable(type);
        }

        return provider;
    }

    /**
     * Get a provider by type, or empty if not available.
     *
     * @param type The provider type
     * @return Optional containing the provider, or empty if not available
     */
    public Optional<OcrProvider> getProviderOptional(OcrProviderType type) {
        if (!ocrProperties.isProviderEnabled(type)) {
            return Optional.empty();
        }

        OcrProvider provider = providers.get(type);
        if (provider == null || !provider.isAvailable()) {
            return Optional.empty();
        }

        return Optional.of(provider);
    }

    /**
     * Get the default configured provider.
     *
     * @return The default provider
     * @throws OcrProcessingException if no default provider is available
     */
    public OcrProvider getDefaultProvider() {
        return getProvider(ocrProperties.getDefaultProvider());
    }

    /**
     * Get a provider with fallback support.
     * Tries providers in order until one is available.
     *
     * @param types The provider types to try in order
     * @return The first available provider
     * @throws OcrProcessingException if no provider is available
     */
    public OcrProvider getProviderWithFallback(OcrProviderType... types) {
        for (OcrProviderType type : types) {
            Optional<OcrProvider> provider = getProviderOptional(type);
            if (provider.isPresent()) {
                return provider.get();
            }
        }

        throw new OcrProcessingException(
                "No OCR provider available from the requested types",
                types.length > 0 ? types[0] : OcrProviderType.TESSERACT);
    }

    /**
     * Get the default provider with configured fallback.
     *
     * @return An available provider (primary or fallback)
     */
    public OcrProvider getDefaultWithFallback() {
        OcrProviderType primary = ocrProperties.getDefaultProvider();

        if (ocrProperties.isFallbackEnabled()) {
            OcrProviderType fallback = ocrProperties.getFallbackProvider();
            return getProviderWithFallback(primary, fallback);
        }

        return getProvider(primary);
    }

    /**
     * Get a provider based on the request preferences.
     * Uses the request's preferred provider if set, otherwise uses default with
     * fallback.
     *
     * @param request The OCR request
     * @return The appropriate provider
     */
    public OcrProvider getProviderForRequest(OcrRequest request) {
        if (request.getPreferredProvider() != null) {
            if (request.isFallbackEnabled() && ocrProperties.isFallbackEnabled()) {
                return getProviderWithFallback(
                        request.getPreferredProvider(),
                        ocrProperties.getFallbackProvider());
            }
            return getProvider(request.getPreferredProvider());
        }

        return getDefaultWithFallback();
    }

    /**
     * Check if a specific provider is available.
     *
     * @param type The provider type to check
     * @return true if the provider is registered, enabled, and available
     */
    public boolean isProviderAvailable(OcrProviderType type) {
        return getProviderOptional(type).isPresent();
    }

    /**
     * Get all available providers.
     *
     * @return List of available providers
     */
    public List<OcrProvider> getAvailableProviders() {
        return providers.values().stream()
                .filter(p -> ocrProperties.isProviderEnabled(p.getProviderType()))
                .filter(OcrProvider::isAvailable)
                .toList();
    }

    /**
     * Get all registered provider types.
     *
     * @return List of registered provider types
     */
    public List<OcrProviderType> getRegisteredProviderTypes() {
        return List.copyOf(providers.keySet());
    }
}
