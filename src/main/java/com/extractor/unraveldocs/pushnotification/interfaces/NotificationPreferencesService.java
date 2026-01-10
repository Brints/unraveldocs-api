package com.extractor.unraveldocs.pushnotification.interfaces;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.dto.request.UpdatePreferencesRequest;
import com.extractor.unraveldocs.pushnotification.dto.response.NotificationPreferencesResponse;

/**
 * Service interface for managing notification preferences.
 */
public interface NotificationPreferencesService {

    /**
     * Get notification preferences for a user.
     * Creates default preferences if none exist.
     */
    NotificationPreferencesResponse getPreferences(String userId);

    /**
     * Update notification preferences for a user.
     */
    NotificationPreferencesResponse updatePreferences(String userId, UpdatePreferencesRequest request);

    /**
     * Check if a specific notification type is enabled for a user.
     */
    boolean isNotificationTypeEnabled(String userId, NotificationType type);

    /**
     * Check if the user is currently in quiet hours.
     */
    boolean isInQuietHours(String userId);

    /**
     * Check if push notifications are globally enabled for a user.
     */
    boolean isPushEnabled(String userId);

    /**
     * Create default preferences for a new user.
     */
    void createDefaultPreferences(String userId);
}
