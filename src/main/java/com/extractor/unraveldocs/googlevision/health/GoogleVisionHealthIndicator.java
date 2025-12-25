package com.extractor.unraveldocs.googlevision.health;

import com.extractor.unraveldocs.googlevision.config.GoogleVisionProperties;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Health checker for Google Cloud Vision API.
 * Provides connectivity and API availability status.
 * Can be used by custom health endpoints or monitoring systems.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ocr.google-vision.enabled", havingValue = "true")
@ConditionalOnBean(ImageAnnotatorClient.class)
public class GoogleVisionHealthIndicator {

    private final ImageAnnotatorClient visionClient;
    private final GoogleVisionProperties properties;

    // Cache health status for a short period to avoid excessive API calls
    private final AtomicReference<CachedHealth> cachedHealth = new AtomicReference<>();
    private static final long CACHE_TTL_MS = 60000; // 1 minute

    /**
     * Check Vision API health and return status details.
     *
     * @return Map containing health status details
     */
    public Map<String, Object> checkHealth() {
        try {
            CachedHealth cached = cachedHealth.get();
            if (cached != null && !cached.isExpired()) {
                return cached.healthDetails();
            }

            Map<String, Object> newHealth = performHealthCheck();
            cachedHealth.set(new CachedHealth(newHealth, System.currentTimeMillis()));
            return newHealth;

        } catch (Exception e) {
            log.warn("Health check failed for Google Cloud Vision: {}", e.getMessage());
            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put("status", "DOWN");
            errorHealth.put("error", e.getMessage());
            errorHealth.put("provider", "google-vision");
            cachedHealth.set(new CachedHealth(errorHealth, System.currentTimeMillis()));
            return errorHealth;
        }
    }

    /**
     * Check if Vision API is healthy.
     *
     * @return true if the API is reachable and responding
     */
    public boolean isHealthy() {
        Map<String, Object> health = checkHealth();
        return "UP".equals(health.get("status"));
    }

    private Map<String, Object> performHealthCheck() {
        Map<String, Object> details = new HashMap<>();
        details.put("provider", "google-vision");

        if (visionClient == null) {
            details.put("status", "DOWN");
            details.put("reason", "ImageAnnotatorClient not initialized");
            return details;
        }

        try {
            // Create a minimal test request with a 1x1 white pixel PNG
            byte[] testImageBytes = createMinimalTestImage();

            Image image = Image.newBuilder()
                    .setContent(ByteString.copyFrom(testImageBytes))
                    .build();

            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .setMaxResults(1)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .setImage(image)
                    .addFeatures(feature)
                    .build();

            // Execute a lightweight API call
            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(
                    List.of(request));

            if (!response.getResponsesList().isEmpty()) {
                AnnotateImageResponse annotateResponse = response.getResponses(0);
                if (annotateResponse.hasError()) {
                    // API responded but with an error - still means connectivity is working
                    details.put("status", "DOWN");
                    details.put("error", annotateResponse.getError().getMessage());
                    details.put("errorCode", annotateResponse.getError().getCode());
                    return details;
                }

                details.put("status", "UP");
                details.put("timeoutSeconds", properties.getTimeoutSeconds());
                details.put("maxRetries", properties.getMaxRetries());
                return details;
            }

            details.put("status", "UP");
            return details;

        } catch (Exception e) {
            log.warn("Vision API health check failed: {}", e.getMessage());

            // Distinguish between connectivity issues and other errors
            String errorMessage = e.getMessage();
            String errorType = "unknown";

            if (errorMessage != null) {
                if (errorMessage.contains("UNAVAILABLE")) {
                    errorType = "service_unavailable";
                } else if (errorMessage.contains("PERMISSION_DENIED")) {
                    errorType = "permission_denied";
                } else if (errorMessage.contains("UNAUTHENTICATED")) {
                    errorType = "unauthenticated";
                } else if (errorMessage.contains("RESOURCE_EXHAUSTED")) {
                    errorType = "rate_limited";
                }
            }

            details.put("status", "DOWN");
            details.put("error", errorMessage);
            details.put("errorType", errorType);
            return details;
        }
    }

    /**
     * Create a minimal 1x1 white pixel PNG for health check.
     */
    private byte[] createMinimalTestImage() {
        // Minimal valid 1x1 white PNG
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
                0x00, 0x00, 0x00, 0x0D, // IHDR length
                0x49, 0x48, 0x44, 0x52, // IHDR
                0x00, 0x00, 0x00, 0x01, // width = 1
                0x00, 0x00, 0x00, 0x01, // height = 1
                0x08, 0x02, // bit depth = 8, color type = 2 (RGB)
                0x00, 0x00, 0x00, // compression, filter, interlace
                (byte) 0x90, 0x77, 0x53, (byte) 0xDE, // IHDR CRC
                0x00, 0x00, 0x00, 0x0C, // IDAT length
                0x49, 0x44, 0x41, 0x54, // IDAT
                0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00,
                0x05, (byte) 0xFE, 0x02, (byte) 0xFE,
                (byte) 0xA3, (byte) 0x21, (byte) 0x00, (byte) 0x00, // IDAT CRC (approximate)
                0x00, 0x00, 0x00, 0x00, // IEND length
                0x49, 0x45, 0x4E, 0x44, // IEND
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82 // IEND CRC
        };
    }

    /**
     * Cached health result with TTL.
     */
    private record CachedHealth(Map<String, Object> healthDetails, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
