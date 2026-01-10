package com.extractor.unraveldocs.pushnotification.kafka;

import com.extractor.unraveldocs.pushnotification.config.NotificationConfig;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationProviderType;
import com.extractor.unraveldocs.pushnotification.interfaces.DeviceTokenService;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationPreferencesService;
import com.extractor.unraveldocs.pushnotification.model.Notification;
import com.extractor.unraveldocs.pushnotification.model.UserDeviceToken;
import com.extractor.unraveldocs.pushnotification.provider.NotificationProviderService;
import com.extractor.unraveldocs.pushnotification.repository.NotificationRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Kafka consumer for processing notification events.
 * Only active when KafkaTemplate bean is available (i.e., when Kafka is
 * configured).
 */
@Slf4j
@Component
@ConditionalOnBean(KafkaTemplate.class)
public class NotificationKafkaConsumer {

    private final NotificationConfig config;
    private final NotificationPreferencesService preferencesService;
    private final DeviceTokenService deviceTokenService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Map<NotificationProviderType, NotificationProviderService> providers;

    @Autowired
    public NotificationKafkaConsumer(
            NotificationConfig config,
            NotificationPreferencesService preferencesService,
            DeviceTokenService deviceTokenService,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            List<NotificationProviderService> providerList) {
        this.config = config;
        this.preferencesService = preferencesService;
        this.deviceTokenService = deviceTokenService;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;

        // Build a map of providers by type
        this.providers = new java.util.HashMap<>();
        for (NotificationProviderService provider : providerList) {
            if (provider.isEnabled()) {
                NotificationProviderType type = NotificationProviderType.valueOf(provider.getProviderName());
                providers.put(type, provider);
                log.info("Registered notification provider: {}", provider.getProviderName());
            }
        }
    }

    @KafkaListener(topics = "${notification.kafka-topic:notification-events}", groupId = "notification-processor", containerFactory = "kafkaListenerContainerFactory")
    public void handleNotificationEvent(NotificationEvent event) {
        log.debug("Received notification event for user {}: {}", event.getUserId(), event.getType());

        try {
            // Check if notification type is enabled for user
            if (!preferencesService.isNotificationTypeEnabled(event.getUserId(), event.getType())) {
                log.debug("Notification type {} disabled for user {}", event.getType(), event.getUserId());
                return;
            }

            // Check quiet hours
            if (config.isRespectQuietHours() && preferencesService.isInQuietHours(event.getUserId())) {
                log.debug("User {} is in quiet hours, skipping notification", event.getUserId());
                return;
            }

            // Persist notification to database
            if (config.isPersistNotifications()) {
                persistNotification(event);
            }

            // Get the active provider
            NotificationProviderService provider = providers.get(config.getActiveProvider());
            if (provider == null || !provider.isEnabled()) {
                log.warn("Active notification provider {} is not available", config.getActiveProvider());
                return;
            }

            // Get user's device tokens and send notification
            List<UserDeviceToken> tokens = deviceTokenService.getActiveTokenEntities(event.getUserId());
            if (tokens.isEmpty()) {
                log.debug("No active device tokens for user {}", event.getUserId());
                return;
            }

            List<String> tokenStrings = tokens.stream()
                    .map(UserDeviceToken::getDeviceToken)
                    .toList();

            int sent = provider.sendBatch(tokenStrings, event.getTitle(), event.getMessage(), event.getData());
            log.info("Sent {} notifications to user {} via {}", sent, event.getUserId(), provider.getProviderName());

        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage(), e);
        }
    }

    private void persistNotification(NotificationEvent event) {
        try {
            Optional<User> userOpt = userRepository.findById(event.getUserId());
            if (userOpt.isEmpty()) {
                log.warn("User not found for notification: {}", event.getUserId());
                return;
            }

            Notification notification = Notification.builder()
                    .user(userOpt.get())
                    .type(event.getType())
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .data(event.getData())
                    .build();

            notificationRepository.save(notification);
            log.debug("Notification persisted for user {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to persist notification: {}", e.getMessage());
        }
    }
}
