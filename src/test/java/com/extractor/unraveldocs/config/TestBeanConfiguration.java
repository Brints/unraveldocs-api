package com.extractor.unraveldocs.config;

import com.extractor.unraveldocs.brokers.kafka.events.BaseEvent;
import com.extractor.unraveldocs.brokers.kafka.events.EventPublisherService;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.dto.response.NotificationResponse;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Test configuration that provides stub implementations for beans
 * that are conditionally created based on external service availability.
 * This ensures tests can run without requiring Kafka, Firebase, etc.
 */
@Slf4j
@Configuration
@Profile("test")
public class TestBeanConfiguration {

    /**
     * Provides a mock EventPublisherService when Kafka is not configured.
     * Uses Mockito to create a mock that can be used in integration tests.
     */
    @Bean
    @ConditionalOnMissingBean(EventPublisherService.class)
    public EventPublisherService stubEventPublisherService() {
        log.info("Creating mock EventPublisherService for test profile");
        return Mockito.mock(EventPublisherService.class);
    }

    /**
     * Provides a stub NotificationService when push notifications are not
     * configured.
     * This bean is only created if the real NotificationService is missing.
     */
    @Bean
    @ConditionalOnMissingBean(NotificationService.class)
    public NotificationService stubNotificationService() {
        log.info("Creating stub NotificationService for test profile");
        return new StubNotificationService();
    }

    /**
     * Stub implementation of NotificationService that logs notifications instead of
     * sending them.
     */
    @Slf4j
    public static class StubNotificationService implements NotificationService {

        @Override
        public void sendToUser(String userId, NotificationType type, String title,
                String message, Map<String, String> data) {
            log.debug("STUB: Would send notification to user");
        }

        @Override
        public void sendToUsers(List<String> userIds, NotificationType type,
                String title, String message, Map<String, String> data) {
            log.debug("STUB: Would send notification to users");
        }

        @Override
        public void sendToTopic(String topic, NotificationType type, String title,
                String message, Map<String, String> data) {
            log.debug("STUB: Would send notification to topic");
        }

        @Override
        public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
            return new PageImpl<>(Collections.emptyList());
        }

        @Override
        public Page<NotificationResponse> getUnreadNotifications(String userId, Pageable pageable) {
            return new PageImpl<>(Collections.emptyList());
        }

        @Override
        public Page<NotificationResponse> getNotificationsByType(String userId,
                NotificationType type, Pageable pageable) {
            return new PageImpl<>(Collections.emptyList());
        }

        @Override
        public long getUnreadCount(String userId) {
            return 0;
        }

        @Override
        public void markAsRead(String userId, String notificationId) {
            log.debug("STUB: Would mark notification as read for user");
        }

        @Override
        public void markAllAsRead(String userId) {
            log.debug("STUB: Would mark all notifications as read for user");
        }

        @Override
        public void deleteNotification(String userId, String notificationId) {
            log.debug("STUB: Would delete notification for user");
        }

        @Override
        public boolean isNotificationTypeEnabled(String userId, NotificationType type) {
            return true; // Always enabled in tests
        }
    }
}
