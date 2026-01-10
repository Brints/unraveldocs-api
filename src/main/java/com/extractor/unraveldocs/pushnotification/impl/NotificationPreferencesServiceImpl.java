package com.extractor.unraveldocs.pushnotification.impl;

import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.dto.request.UpdatePreferencesRequest;
import com.extractor.unraveldocs.pushnotification.dto.response.NotificationPreferencesResponse;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationPreferencesService;
import com.extractor.unraveldocs.pushnotification.model.NotificationPreferences;
import com.extractor.unraveldocs.pushnotification.repository.NotificationPreferencesRepository;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of NotificationPreferencesService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferencesServiceImpl implements NotificationPreferencesService {

    private final NotificationPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public NotificationPreferencesResponse getPreferences(String userId) {
        NotificationPreferences preferences = getOrCreatePreferences(userId);
        return mapToResponse(preferences);
    }

    @Override
    @Transactional
    public NotificationPreferencesResponse updatePreferences(String userId, UpdatePreferencesRequest request) {
        NotificationPreferences preferences = getOrCreatePreferences(userId);

        preferences.setPushEnabled(request.getPushEnabled());
        preferences.setEmailEnabled(request.getEmailEnabled());
        preferences.setDocumentNotifications(request.getDocumentNotifications());
        preferences.setOcrNotifications(request.getOcrNotifications());
        preferences.setPaymentNotifications(request.getPaymentNotifications());
        preferences.setStorageNotifications(request.getStorageNotifications());
        preferences.setSubscriptionNotifications(request.getSubscriptionNotifications());
        preferences.setTeamNotifications(request.getTeamNotifications());

        if (request.getQuietHoursEnabled() != null) {
            preferences.setQuietHoursEnabled(request.getQuietHoursEnabled());
        }
        if (request.getQuietHoursStart() != null) {
            preferences.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            preferences.setQuietHoursEnd(request.getQuietHoursEnd());
        }

        NotificationPreferences saved = preferencesRepository.save(preferences);
        log.info("Updated notification preferences for user {}", userId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNotificationTypeEnabled(String userId, NotificationType type) {
        return preferencesRepository.findByUserId(userId)
                .map(prefs -> prefs.isNotificationTypeEnabled(type))
                .orElse(true); // Default to enabled if no preferences exist
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInQuietHours(String userId) {
        return preferencesRepository.findByUserId(userId)
                .map(NotificationPreferences::isInQuietHours)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPushEnabled(String userId) {
        return preferencesRepository.findByUserId(userId)
                .map(NotificationPreferences::isPushEnabled)
                .orElse(true);
    }

    @Override
    @Transactional
    public void createDefaultPreferences(String userId) {
        if (!preferencesRepository.existsByUserId(userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            NotificationPreferences preferences = NotificationPreferences.createDefault(user);
            preferencesRepository.save(preferences);
            log.info("Created default notification preferences for user {}", userId);
        }
    }

    private NotificationPreferences getOrCreatePreferences(String userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

                    NotificationPreferences newPrefs = NotificationPreferences.createDefault(user);
                    NotificationPreferences saved = preferencesRepository.save(newPrefs);
                    log.info("Created default notification preferences for user {}", userId);
                    return saved;
                });
    }

    private NotificationPreferencesResponse mapToResponse(NotificationPreferences preferences) {
        return NotificationPreferencesResponse.builder()
                .id(preferences.getId())
                .pushEnabled(preferences.isPushEnabled())
                .emailEnabled(preferences.isEmailEnabled())
                .documentNotifications(preferences.isDocumentNotifications())
                .ocrNotifications(preferences.isOcrNotifications())
                .paymentNotifications(preferences.isPaymentNotifications())
                .storageNotifications(preferences.isStorageNotifications())
                .subscriptionNotifications(preferences.isSubscriptionNotifications())
                .teamNotifications(preferences.isTeamNotifications())
                .quietHoursEnabled(preferences.isQuietHoursEnabled())
                .quietHoursStart(preferences.getQuietHoursStart())
                .quietHoursEnd(preferences.getQuietHoursEnd())
                .createdAt(preferences.getCreatedAt())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }
}
