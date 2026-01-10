package com.extractor.unraveldocs.pushnotification.provider.sns;

import com.extractor.unraveldocs.pushnotification.config.AwsSnsConfig;
import com.extractor.unraveldocs.pushnotification.provider.NotificationProviderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS SNS implementation of NotificationProviderService.
 * Uses AWS SNS to send push notifications to mobile devices.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "aws.sns.enabled", havingValue = "true", matchIfMissing = false)
public class AwsSnsNotificationProvider implements NotificationProviderService {

    private final AwsSnsConfig config;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public AwsSnsNotificationProvider(
            AwsSnsConfig config,
            @Qualifier("snsPushClient") SnsClient snsClient,
            ObjectMapper objectMapper) {
        this.config = config;
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        log.info("AWS SNS Notification Provider initialized");
    }

    @Override
    public boolean send(String endpointArn, String title, String message, Map<String, String> data) {
        try {
            String messagePayload = buildMessagePayload(title, message, data);

            PublishRequest request = PublishRequest.builder()
                    .targetArn(endpointArn)
                    .message(messagePayload)
                    .messageStructure("json")
                    .build();

            PublishResponse response = snsClient.publish(request);
            log.debug("SNS message sent: {}", response.messageId());
            return true;
        } catch (SnsException e) {
            log.error("Failed to send SNS notification: {}", e.getMessage());
            handleSnsException(e, endpointArn);
            return false;
        }
    }

    @Override
    public int sendBatch(List<String> endpointArns, String title, String message, Map<String, String> data) {
        int successCount = 0;
        for (String endpointArn : endpointArns) {
            if (send(endpointArn, title, message, data)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public boolean sendToTopic(String topicArn, String title, String message, Map<String, String> data) {
        try {
            String messagePayload = buildMessagePayload(title, message, data);

            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(messagePayload)
                    .messageStructure("json")
                    .build();

            PublishResponse response = snsClient.publish(request);
            log.debug("SNS topic message sent: {}", response.messageId());
            return true;
        } catch (SnsException e) {
            log.error("Failed to send SNS topic notification: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "AWS_SNS";
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public boolean subscribeToTopic(String endpointArn, String topicArn) {
        try {
            SubscribeRequest request = SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("application")
                    .endpoint(endpointArn)
                    .build();

            SubscribeResponse response = snsClient.subscribe(request);
            log.debug("Subscribed {} to topic {}: {}", endpointArn, topicArn, response.subscriptionArn());
            return true;
        } catch (SnsException e) {
            log.error("Failed to subscribe to topic: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean unsubscribeFromTopic(String subscriptionArn, String topic) {
        try {
            UnsubscribeRequest request = UnsubscribeRequest.builder()
                    .subscriptionArn(subscriptionArn)
                    .build();

            snsClient.unsubscribe(request);
            log.debug("Unsubscribed: {}", subscriptionArn);
            return true;
        } catch (SnsException e) {
            log.error("Failed to unsubscribe: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Register a device token with SNS and get an endpoint ARN.
     */
    public String createPlatformEndpoint(String deviceToken) {
        try {
            CreatePlatformEndpointRequest request = CreatePlatformEndpointRequest.builder()
                    .platformApplicationArn(config.getPlatformApplicationArn())
                    .token(deviceToken)
                    .build();

            CreatePlatformEndpointResponse response = snsClient.createPlatformEndpoint(request);
            return response.endpointArn();
        } catch (SnsException e) {
            log.error("Failed to create platform endpoint: {}", e.getMessage());
            return null;
        }
    }

    private String buildMessagePayload(String title, String message, Map<String, String> data) {
        try {
            // Build platform-specific payloads
            Map<String, Object> gcmPayload = new HashMap<>();
            Map<String, Object> gcmNotification = new HashMap<>();
            gcmNotification.put("title", title);
            gcmNotification.put("body", message);
            gcmPayload.put("notification", gcmNotification);
            if (data != null) {
                gcmPayload.put("data", data);
            }

            Map<String, Object> apnsPayload = new HashMap<>();
            Map<String, Object> aps = new HashMap<>();
            Map<String, Object> alert = new HashMap<>();
            alert.put("title", title);
            alert.put("body", message);
            aps.put("alert", alert);
            aps.put("sound", "default");
            apnsPayload.put("aps", aps);
            if (data != null) {
                apnsPayload.putAll(data);
            }

            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("default", message);
            messageMap.put("GCM", objectMapper.writeValueAsString(gcmPayload));
            messageMap.put("APNS", objectMapper.writeValueAsString(apnsPayload));
            messageMap.put("APNS_SANDBOX", objectMapper.writeValueAsString(apnsPayload));

            return objectMapper.writeValueAsString(messageMap);
        } catch (JsonProcessingException e) {
            log.error("Failed to build message payload: {}", e.getMessage());
            return message;
        }
    }

    private void handleSnsException(SnsException e, String endpointArn) {
        if (e.awsErrorDetails() != null) {
            String errorCode = e.awsErrorDetails().errorCode();
            if ("EndpointDisabled".equals(errorCode) || "InvalidParameter".equals(errorCode)) {
                log.warn("Invalid or disabled endpoint, should be removed: {}", endpointArn);
            }
        }
    }
}
