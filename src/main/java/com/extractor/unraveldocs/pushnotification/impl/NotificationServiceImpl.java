package com.extractor.unraveldocs.pushnotification.impl;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.dto.response.NotificationResponse;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationPreferencesService;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.pushnotification.kafka.NotificationKafkaProducer;
import com.extractor.unraveldocs.pushnotification.model.Notification;
import com.extractor.unraveldocs.pushnotification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Implementation of NotificationService.
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationKafkaProducer kafkaProducer;
    private final NotificationPreferencesService preferencesService;

    @Autowired
    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            @Autowired(required = false) NotificationKafkaProducer kafkaProducer,
            NotificationPreferencesService preferencesService) {
        this.notificationRepository = notificationRepository;
        this.kafkaProducer = kafkaProducer;
        this.preferencesService = preferencesService;

        if (kafkaProducer == null) {
            log.warn("NotificationKafkaProducer not available - notifications will be logged only");
        }
    }

    @Override
    public void sendToUser(String userId, NotificationType type, String title,
            String message, Map<String, String> data) {
        log.debug("Sending notification to user {}: {} - {}", userId, type, title);
        if (kafkaProducer != null) {
            kafkaProducer.publishNotification(userId, type, title, message, data);
        } else {
            log.info("Would send notification to user {}: {} - {}", userId, type, title);
        }
    }

    @Override
    public void sendToUsers(List<String> userIds, NotificationType type, String title,
            String message, Map<String, String> data) {
        log.debug("Sending notification to {} users: {} - {}", userIds.size(), type, title);
        if (kafkaProducer != null) {
            kafkaProducer.publishNotifications(userIds, type, title, message, data);
        } else {
            log.info("Would send notification to {} users: {} - {}", userIds.size(), type, title);
        }
    }

    @Override
    public void sendToTopic(String topic, NotificationType type, String title,
            String message, Map<String, String> data) {
        log.debug("Sending notification to topic {}: {} - {}", topic, type, title);
        // Topic notifications would be sent directly through the provider
        // This would require a different flow for topic-based messaging
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByType(String userId, NotificationType type,
            Pageable pageable) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(String userId, String notificationId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId))
                .ifPresent(notification -> {
                    notification.markAsRead();
                    notificationRepository.save(notification);
                    log.debug("Marked notification {} as read for user {}", notificationId, userId);
                });
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        int updated = notificationRepository.markAllAsRead(userId, OffsetDateTime.now());
        log.debug("Marked {} notifications as read for user {}", updated, userId);
    }

    @Override
    @Transactional
    public void deleteNotification(String userId, String notificationId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId))
                .ifPresent(notification -> {
                    notificationRepository.delete(notification);
                    log.debug("Deleted notification {} for user {}", notificationId, userId);
                });
    }

    @Override
    public boolean isNotificationTypeEnabled(String userId, NotificationType type) {
        return preferencesService.isNotificationTypeEnabled(userId, type);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .typeDisplayName(notification.getType().getDisplayName())
                .category(notification.getType().getCategory())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
