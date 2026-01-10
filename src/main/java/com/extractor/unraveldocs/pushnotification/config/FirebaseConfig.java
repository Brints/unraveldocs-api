package com.extractor.unraveldocs.pushnotification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for Firebase Cloud Messaging.
 * Initializes the Firebase Admin SDK for sending push notifications.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.credentials.json:}")
    private String credentialsJson;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(getCredentials())
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
            } else {
                log.info("Firebase Admin SDK already initialized");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }

    private GoogleCredentials getCredentials() throws IOException {
        // Priority 1: JSON content directly in environment variable
        if (StringUtils.hasText(credentialsJson)) {
            log.info("Loading Firebase credentials from JSON environment variable");
            InputStream stream = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            return GoogleCredentials.fromStream(stream);
        }

        // Priority 2: File path (classpath or filesystem)
        if (StringUtils.hasText(credentialsPath)) {
            log.info("Loading Firebase credentials from path: {}", credentialsPath);
            Resource resource;

            if (credentialsPath.startsWith("classpath:")) {
                String path = credentialsPath.substring("classpath:".length());
                resource = new ClassPathResource(path);
            } else if (credentialsPath.startsWith("file:")) {
                String path = credentialsPath.substring("file:".length());
                resource = new FileSystemResource(path);
            } else {
                // Try classpath first, then filesystem
                resource = new ClassPathResource(credentialsPath);
                if (!resource.exists()) {
                    resource = new FileSystemResource(credentialsPath);
                }
            }

            if (resource.exists()) {
                return GoogleCredentials.fromStream(resource.getInputStream());
            }
            throw new IOException("Firebase credentials file not found: " + credentialsPath);
        }

        // Priority 3: Application Default Credentials (ADC)
        log.info("Using Application Default Credentials for Firebase");
        return GoogleCredentials.getApplicationDefault();
    }
}
