package com.extractor.unraveldocs.pushnotification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Response DTO for notification preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferencesResponse {

    private String id;
    private boolean pushEnabled;
    private boolean emailEnabled;
    private boolean documentNotifications;
    private boolean ocrNotifications;
    private boolean paymentNotifications;
    private boolean storageNotifications;
    private boolean subscriptionNotifications;
    private boolean teamNotifications;
    private boolean quietHoursEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
