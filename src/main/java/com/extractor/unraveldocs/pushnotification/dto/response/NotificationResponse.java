package com.extractor.unraveldocs.pushnotification.dto.response;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Response DTO for notification data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private String id;
    private NotificationType type;
    private String typeDisplayName;
    private String category;
    private String title;
    private String message;
    private Map<String, String> data;
    private boolean isRead;
    private OffsetDateTime createdAt;
    private OffsetDateTime readAt;
}
