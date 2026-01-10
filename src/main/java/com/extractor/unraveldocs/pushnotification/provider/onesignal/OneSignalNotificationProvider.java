package com.extractor.unraveldocs.pushnotification.provider.onesignal;

import com.extractor.unraveldocs.pushnotification.config.OneSignalConfig;
import com.extractor.unraveldocs.pushnotification.provider.NotificationProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OneSignal implementation of NotificationProviderService.
 * Uses OneSignal REST API to send push notifications.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "onesignal.enabled", havingValue = "true", matchIfMissing = false)
public class OneSignalNotificationProvider implements NotificationProviderService {

    private final OneSignalConfig config;
    private final RestTemplate restTemplate;

    @Autowired
    public OneSignalNotificationProvider(
            OneSignalConfig config,
            @Qualifier("oneSignalRestTemplate") RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
        log.info("OneSignal Notification Provider initialized");
    }

    @Override
    public boolean send(String deviceToken, String title, String message, Map<String, String> data) {
        return sendBatch(List.of(deviceToken), title, message, data) > 0;
    }

    @Override
    public int sendBatch(List<String> deviceTokens, String title, String message, Map<String, String> data) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return 0;
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("app_id", config.getAppId());
            requestBody.put("include_player_ids", deviceTokens);
            requestBody.put("headings", Map.of("en", title));
            requestBody.put("contents", Map.of("en", message));

            if (data != null && !data.isEmpty()) {
                requestBody.put("data", data);
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getApiUrl() + "/notifications",
                    HttpMethod.POST,
                    entity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object recipients = response.getBody().get("recipients");
                int recipientCount = recipients instanceof Number ? ((Number) recipients).intValue()
                        : deviceTokens.size();
                log.debug("OneSignal notification sent to {} recipients", recipientCount);
                return recipientCount;
            }

            return 0;
        } catch (Exception e) {
            log.error("Failed to send OneSignal notification: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean sendToTopic(String topic, String title, String message, Map<String, String> data) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("app_id", config.getAppId());
            requestBody.put("included_segments", List.of(topic));
            requestBody.put("headings", Map.of("en", title));
            requestBody.put("contents", Map.of("en", message));

            if (data != null && !data.isEmpty()) {
                requestBody.put("data", data);
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getApiUrl() + "/notifications",
                    HttpMethod.POST,
                    entity,
                    Map.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.debug("OneSignal topic notification sent to segment: {}", topic);
            }
            return success;
        } catch (Exception e) {
            log.error("Failed to send OneSignal topic notification: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "OneSignal";
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public boolean subscribeToTopic(String deviceToken, String topic) {
        // OneSignal uses segments/tags rather than topic subscriptions
        // This would be implemented using the Edit Device endpoint
        log.warn("OneSignal topic subscription not implemented - use tags instead");
        return false;
    }

    @Override
    public boolean unsubscribeFromTopic(String deviceToken, String topic) {
        log.warn("OneSignal topic unsubscription not implemented - use tags instead");
        return false;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + config.getApiKey());
        return headers;
    }
}
