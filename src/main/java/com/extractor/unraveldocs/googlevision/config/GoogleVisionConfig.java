package com.extractor.unraveldocs.googlevision.config;

import com.google.cloud.vision.v1.ImageAnnotatorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Configuration for Google Cloud Vision API.
 * Creates the ImageAnnotatorClient bean when Vision is enabled.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ocr.google-vision.enabled", havingValue = "true")
public class GoogleVisionConfig {

    private final GoogleVisionProperties properties;

    /**
     * Creates the Google Cloud Vision ImageAnnotatorClient.
     * Uses default application credentials from the environment.
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
            ImageAnnotatorClient client = ImageAnnotatorClient.create();
            log.info("Google Cloud Vision ImageAnnotatorClient created successfully");
            return client;
        } catch (IOException e) {
            log.error("Failed to create ImageAnnotatorClient: {}", e.getMessage(), e);
            throw e;
        }
    }
}
