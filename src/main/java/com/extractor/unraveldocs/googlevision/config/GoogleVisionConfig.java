package com.extractor.unraveldocs.googlevision.config;

import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.threeten.bp.Duration;

import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration for Google Cloud Vision API.
 * Creates the ImageAnnotatorClient bean when Vision is enabled.
 * 
 * Credentials loading order:
 * 1. Property: ocr.google-vision.credentials-location (classpath: or file:
 * prefix supported)
 * 2. Environment variable: GOOGLE_APPLICATION_CREDENTIALS
 * 3. Application Default Credentials (ADC) - for GKE/Cloud Run with Workload
 * Identity
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ocr.google-vision.enabled", havingValue = "true")
public class GoogleVisionConfig {

    private final GoogleVisionProperties properties;

    /**
     * Creates the Google Cloud Vision ImageAnnotatorClient.
     * Loads credentials from configured location or falls back to ADC.
     *
     * @return Configured ImageAnnotatorClient
     * @throws IOException if client creation fails
     */
    @Bean
    public ImageAnnotatorClient imageAnnotatorClient() throws IOException {
        log.info("Initializing Google Cloud Vision ImageAnnotatorClient");
        log.info("Vision API settings - timeout: {}s, maxRetries: {}, detectLanguage: {}",
                properties.getTimeoutSeconds(),
                properties.getMaxRetries(),
                properties.isDetectLanguage());

        try {
            ImageAnnotatorSettings.Builder settingsBuilder = ImageAnnotatorSettings.newBuilder();

            // Configure retry settings
            settingsBuilder
                    .batchAnnotateImagesSettings()
                    .setRetrySettings(
                            RetrySettings.newBuilder()
                                    .setInitialRpcTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                                    .setMaxRpcTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                                    .setMaxAttempts(properties.getMaxRetries())
                                    .build());

            // Load credentials
            GoogleCredentials credentials = loadCredentials();
            if (credentials != null) {
                settingsBuilder.setCredentialsProvider(() -> credentials);
                log.info("Using credentials from configured location");
            } else {
                log.info("Using Application Default Credentials (ADC)");
            }

            ImageAnnotatorClient client = ImageAnnotatorClient.create(settingsBuilder.build());
            log.info("Google Cloud Vision ImageAnnotatorClient created successfully");
            return client;
        } catch (IOException e) {
            log.error("Failed to create ImageAnnotatorClient: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Load Google Cloud credentials from configured location.
     * 
     * @return GoogleCredentials or null to use ADC
     * @throws IOException if credentials file cannot be read
     */
    private GoogleCredentials loadCredentials() throws IOException {
        String credentialsLocation = properties.getCredentialsLocation();

        if (credentialsLocation == null || credentialsLocation.isBlank()) {
            log.debug("No credentials-location configured, will use ADC");
            return null;
        }

        log.info("Loading Google Cloud credentials from: {}", credentialsLocation);

        Resource resource = resolveResource(credentialsLocation);

        if (!resource.exists()) {
            log.warn("Credentials file not found at: {}. Falling back to ADC.", credentialsLocation);
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
            log.info("Successfully loaded credentials from: {}", credentialsLocation);
            return credentials;
        }
    }

    /**
     * Resolve a resource from a location string.
     * Supports classpath:, file:, and absolute paths.
     */
    private Resource resolveResource(String location) {
        if (location.startsWith("classpath:")) {
            String path = location.substring("classpath:".length());
            return new ClassPathResource(path);
        } else if (location.startsWith("file:")) {
            String path = location.substring("file:".length());
            return new FileSystemResource(path);
        } else {
            // Assume it's an absolute or relative file path
            return new FileSystemResource(location);
        }
    }
}
