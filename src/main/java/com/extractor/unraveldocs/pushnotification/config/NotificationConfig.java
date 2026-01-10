package com.extractor.unraveldocs.pushnotification.config;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationProviderType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Main notification configuration.
 * Specifies which provider is active and other global settings.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "notification")
public class NotificationConfig {

    /**
     * The active notification provider.
     * Options: FCM, ONESIGNAL, AWS_SNS
     */
    private NotificationProviderType activeProvider = NotificationProviderType.FCM;

    /**
     * Whether to store notifications in the database.
     */
    private boolean persistNotifications = true;

    /**
     * Whether to respect user quiet hours.
     */
    private boolean respectQuietHours = true;

    /**
     * Maximum number of devices per user.
     */
    private int maxDevicesPerUser = 10;

    /**
     * Number of days to keep notifications before cleanup.
     */
    private int notificationRetentionDays = 90;

    /**
     * Kafka topic for notification events.
     */
    private String kafkaTopic = "notification-events";
}
