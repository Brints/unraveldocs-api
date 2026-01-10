package com.extractor.unraveldocs.pushnotification.interfaces;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Service interface for managing notifications.
 */
public interface NotificationService {

    /**
     * Send a notification to a specific user.
     */
    void sendToUser(String userId, NotificationType type, String title, String message, Map<String, String> data);

    /**
     * Send a notification to multiple users.
     */
    void sendToUsers(java.util.List<String> userIds, NotificationType type, String title, String message,
            Map<String, String> data);

    /**
     * Send a notification to a topic (all subscribed users).
     */
    void sendToTopic(String topic, NotificationType type, String title, String message, Map<String, String> data);

    /**
     * Get all notifications for a user.
     */
    Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable);

    /**
     * Get notifications for a user filtered by type.
     */
    Page<NotificationResponse> getNotificationsByType(String userId, NotificationType type, Pageable pageable);

    /**
     * Get unread notifications for a user.
     */
    Page<NotificationResponse> getUnreadNotifications(String userId, Pageable pageable);

    /**
     * Get the count of unread notifications for a user.
     */
    long getUnreadCount(String userId);

    /**
     * Mark a specific notification as read.
     */
    void markAsRead(String userId, String notificationId);

    /**
     * Mark all notifications as read for a user.
     */
    void markAllAsRead(String userId);

    /**
     * Delete a specific notification.
     */
    void deleteNotification(String userId, String notificationId);

    /**
     * Check if a notification type is enabled for a user.
     */
    boolean isNotificationTypeEnabled(String userId, NotificationType type);
}
