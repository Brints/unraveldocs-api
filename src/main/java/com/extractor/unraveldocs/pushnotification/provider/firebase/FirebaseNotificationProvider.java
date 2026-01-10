package com.extractor.unraveldocs.pushnotification.provider.firebase;

import com.extractor.unraveldocs.pushnotification.provider.NotificationProviderService;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) implementation of NotificationProviderService.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseNotificationProvider implements NotificationProviderService {

    private final FirebaseMessaging firebaseMessaging;

    @Value("${firebase.enabled:false}")
    private boolean enabled;

    @Autowired
    public FirebaseNotificationProvider(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
        log.info("Firebase Notification Provider initialized");
    }

    @Override
    public boolean send(String deviceToken, String title, String message, Map<String, String> data) {
        try {
            Message fcmMessage = buildMessage(deviceToken, title, message, data);
            String response = firebaseMessaging.send(fcmMessage);
            log.debug("FCM message sent successfully: {}", response);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to {}: {}", deviceToken, e.getMessage());
            handleFirebaseException(e, deviceToken);
            return false;
        }
    }

    @Override
    public int sendBatch(List<String> deviceTokens, String title, String message, Map<String, String> data) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return 0;
        }

        // FCM allows up to 500 tokens per batch
        int batchSize = 500;
        int successCount = 0;

        for (int i = 0; i < deviceTokens.size(); i += batchSize) {
            List<String> batch = deviceTokens.subList(i, Math.min(i + batchSize, deviceTokens.size()));
            successCount += sendBatchInternal(batch, title, message, data);
        }

        return successCount;
    }

    private int sendBatchInternal(List<String> tokens, String title, String message, Map<String, String> data) {
        try {
            MulticastMessage multicastMessage = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(message)
                            .build())
                    .putAllData(data != null ? data : Map.of())
                    .addAllTokens(tokens)
                    .setAndroidConfig(getAndroidConfig())
                    .setApnsConfig(getApnsConfig())
                    .setWebpushConfig(getWebpushConfig(title, message))
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(multicastMessage);

            log.debug("FCM batch sent: {} success, {} failure",
                    response.getSuccessCount(), response.getFailureCount());

            // Handle failed tokens
            if (response.getFailureCount() > 0) {
                handleBatchFailures(response, tokens);
            }

            return response.getSuccessCount();
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM batch: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean sendToTopic(String topic, String title, String message, Map<String, String> data) {
        try {
            Message fcmMessage = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(message)
                            .build())
                    .putAllData(data != null ? data : Map.of())
                    .setAndroidConfig(getAndroidConfig())
                    .setApnsConfig(getApnsConfig())
                    .setWebpushConfig(getWebpushConfig(title, message))
                    .build();

            String response = firebaseMessaging.send(fcmMessage);
            log.debug("FCM topic message sent to {}: {}", topic, response);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM topic message to {}: {}", topic, e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "FCM";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean subscribeToTopic(String deviceToken, String topic) {
        try {
            TopicManagementResponse response = firebaseMessaging.subscribeToTopic(List.of(deviceToken), topic);
            return response.getSuccessCount() > 0;
        } catch (FirebaseMessagingException e) {
            log.error("Failed to subscribe {} to topic {}: {}", deviceToken, topic, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean unsubscribeFromTopic(String deviceToken, String topic) {
        try {
            TopicManagementResponse response = firebaseMessaging.unsubscribeFromTopic(List.of(deviceToken), topic);
            return response.getSuccessCount() > 0;
        } catch (FirebaseMessagingException e) {
            log.error("Failed to unsubscribe {} from topic {}: {}", deviceToken, topic, e.getMessage());
            return false;
        }
    }

    private Message buildMessage(String token, String title, String message, Map<String, String> data) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(message)
                        .build())
                .putAllData(data != null ? data : Map.of())
                .setAndroidConfig(getAndroidConfig())
                .setApnsConfig(getApnsConfig())
                .setWebpushConfig(getWebpushConfig(title, message))
                .build();
    }

    private AndroidConfig getAndroidConfig() {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setSound("default")
                        .setClickAction("OPEN_ACTIVITY")
                        .build())
                .build();
    }

    private ApnsConfig getApnsConfig() {
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build();
    }

    private WebpushConfig getWebpushConfig(String title, String message) {
        return WebpushConfig.builder()
                .setNotification(WebpushNotification.builder()
                        .setTitle(title)
                        .setBody(message)
                        .setIcon("/icon.png")
                        .build())
                .build();
    }

    private void handleFirebaseException(FirebaseMessagingException e, String token) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        if (errorCode == MessagingErrorCode.UNREGISTERED ||
                errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("Invalid or unregistered token, should be removed: {}", token);
            // Token cleanup would be handled by the calling service
        }
    }

    private void handleBatchFailures(BatchResponse response, List<String> tokens) {
        List<SendResponse> responses = response.getResponses();
        List<String> invalidTokens = new ArrayList<>();

        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                FirebaseMessagingException exception = responses.get(i).getException();
                if (exception != null) {
                    MessagingErrorCode errorCode = exception.getMessagingErrorCode();
                    if (errorCode == MessagingErrorCode.UNREGISTERED ||
                            errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                        invalidTokens.add(tokens.get(i));
                    }
                }
            }
        }

        if (!invalidTokens.isEmpty()) {
            log.warn("Found {} invalid tokens that should be removed", invalidTokens.size());
        }
    }
}
