package com.extractor.unraveldocs.pushnotification.kafka;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.pushnotification.config.NotificationConfig;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Kafka producer for publishing notification events.
 * Only active when KafkaTemplate bean is available (i.e., when Kafka is
 * configured).
 */
@Slf4j
@Component
@ConditionalOnBean(KafkaTemplate.class)
public class NotificationKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotificationConfig notificationConfig;
    private final SanitizeLogging sanitizer;

    @Autowired
    public NotificationKafkaProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            SanitizeLogging sanitizer,
            NotificationConfig notificationConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationConfig = notificationConfig;
        this.sanitizer = sanitizer;
        log.info("NotificationKafkaProducer initialized");
    }

    /**
     * Publish a notification event for a single user.
     */
    public void publishNotification(String userId, NotificationType type, String title,
            String message, Map<String, String> data) {
        NotificationEvent event = NotificationEvent.create(userId, type, title, message, data);
        publishEvent(event);
    }

    /**
     * Publish notification events for multiple users.
     */
    public void publishNotifications(List<String> userIds, NotificationType type, String title,
            String message, Map<String, String> data) {
        for (String userId : userIds) {
            publishNotification(userId, type, title, message, data);
        }
    }

    /**
     * Publish a notification event.
     */
    public void publishEvent(NotificationEvent event) {
        try {
            String topic = notificationConfig.getKafkaTopic();
            kafkaTemplate.send(topic, event.getUserId(), event)
                    .whenComplete((_, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish notification event: {}", ex.getMessage());
                        } else {
                            log.debug("Notification event published for user {}: {}",
                                    sanitizer.sanitizeLogging(event.getUserId()),
                                    sanitizer.sanitizeLoggingObject(event.getType()));
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing notification event: {}", e.getMessage());
        }
    }
}
