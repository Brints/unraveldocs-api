package com.extractor.unraveldocs.pushnotification.provider;

import java.util.List;
import java.util.Map;

/**
 * Interface for push notification providers.
 * All providers (FCM, OneSignal, AWS SNS) implement this interface.
 */
public interface NotificationProviderService {

    /**
     * Send a notification to a single device.
     *
     * @param deviceToken The device token to send to
     * @param title       Notification title
     * @param message     Notification message body
     * @param data        Additional data payload
     * @return true if sent successfully
     */
    boolean send(String deviceToken, String title, String message, Map<String, String> data);

    /**
     * Send a notification to multiple devices.
     *
     * @param deviceTokens List of device tokens
     * @param title        Notification title
     * @param message      Notification message body
     * @param data         Additional data payload
     * @return Number of successful sends
     */
    int sendBatch(List<String> deviceTokens, String title, String message, Map<String, String> data);

    /**
     * Send a notification to a topic.
     *
     * @param topic   Topic name
     * @param title   Notification title
     * @param message Notification message body
     * @param data    Additional data payload
     * @return true if sent successfully
     */
    boolean sendToTopic(String topic, String title, String message, Map<String, String> data);

    /**
     * Get the provider name.
     */
    String getProviderName();

    /**
     * Check if the provider is enabled and configured.
     */
    boolean isEnabled();

    /**
     * Subscribe a device to a topic.
     */
    boolean subscribeToTopic(String deviceToken, String topic);

    /**
     * Unsubscribe a device from a topic.
     */
    boolean unsubscribeFromTopic(String deviceToken, String topic);
}
