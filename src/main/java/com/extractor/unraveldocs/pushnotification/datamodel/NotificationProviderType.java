package com.extractor.unraveldocs.pushnotification.datamodel;

/**
 * Enum representing the available push notification providers.
 * The system supports multiple providers with FCM as the primary.
 */
public enum NotificationProviderType {
    FCM("Firebase Cloud Messaging"),
    ONESIGNAL("OneSignal"),
    AWS_SNS("AWS Simple Notification Service");

    private final String displayName;

    NotificationProviderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
