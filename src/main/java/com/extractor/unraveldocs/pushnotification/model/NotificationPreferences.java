package com.extractor.unraveldocs.pushnotification.model;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Entity representing user notification preferences.
 * Users can enable/disable specific notification categories.
 */
@Data
@Entity
@Table(name = "notification_preferences")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private boolean pushEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    @Column(name = "document_notifications", nullable = false)
    @Builder.Default
    private boolean documentNotifications = true;

    @Column(name = "ocr_notifications", nullable = false)
    @Builder.Default
    private boolean ocrNotifications = true;

    @Column(name = "payment_notifications", nullable = false)
    @Builder.Default
    private boolean paymentNotifications = true;

    @Column(name = "storage_notifications", nullable = false)
    @Builder.Default
    private boolean storageNotifications = true;

    @Column(name = "subscription_notifications", nullable = false)
    @Builder.Default
    private boolean subscriptionNotifications = true;

    @Column(name = "team_notifications", nullable = false)
    @Builder.Default
    private boolean teamNotifications = true;

    @Column(name = "quiet_hours_enabled", nullable = false)
    @Builder.Default
    private boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Check if a notification type is enabled for this user.
     */
    public boolean isNotificationTypeEnabled(NotificationType type) {
        if (!pushEnabled) {
            return false;
        }

        return switch (type.getCategory()) {
            case "document" -> documentNotifications;
            case "ocr" -> ocrNotifications;
            case "payment" -> paymentNotifications;
            case "storage" -> storageNotifications;
            case "subscription" -> subscriptionNotifications;
            case "team" -> teamNotifications;
            case "system" -> true; // System notifications are always enabled
            default -> true;
        };
    }

    /**
     * Check if currently in quiet hours.
     */
    public boolean isInQuietHours() {
        if (!quietHoursEnabled || quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        LocalTime now = LocalTime.now();

        // Handle overnight quiet hours (e.g., 22:00 to 07:00)
        if (quietHoursStart.isAfter(quietHoursEnd)) {
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        }

        return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
    }

    /**
     * Create default preferences for a user.
     */
    public static NotificationPreferences createDefault(User user) {
        return NotificationPreferences.builder()
                .user(user)
                .pushEnabled(true)
                .emailEnabled(true)
                .documentNotifications(true)
                .ocrNotifications(true)
                .paymentNotifications(true)
                .storageNotifications(true)
                .subscriptionNotifications(true)
                .teamNotifications(true)
                .quietHoursEnabled(false)
                .build();
    }
}
