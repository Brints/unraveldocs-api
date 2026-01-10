package com.extractor.unraveldocs.pushnotification.kafka;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Kafka message for notification events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {

    private String id;
    private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private Map<String, String> data;
    private Instant timestamp;

    public static NotificationEvent create(
            String userId,
            NotificationType type,
            String title,
            String message,
            Map<String, String> data) {
        return NotificationEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }
}
