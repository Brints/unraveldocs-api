package com.extractor.unraveldocs.pushnotification.impl;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.pushnotification.dto.request.UpdatePreferencesRequest;
import com.extractor.unraveldocs.pushnotification.interfaces.DeviceTokenService;
import com.extractor.unraveldocs.pushnotification.model.NotificationPreferences;
import com.extractor.unraveldocs.pushnotification.repository.NotificationPreferencesRepository;
import com.extractor.unraveldocs.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferencesServiceImplTest {

    @Mock
    private NotificationPreferencesRepository preferencesRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeviceTokenService deviceTokenService;

    @Mock
    private SanitizeLogging sanitizeLogging;

    @InjectMocks
    private NotificationPreferencesServiceImpl service;

    @Test
    void updatePreferences_whenPushTransitionsToDisabled_deletesAllDeviceTokens() {
        String userId = "user-1";
        NotificationPreferences existing = NotificationPreferences.builder()
                .pushEnabled(true)
                .emailEnabled(true)
                .documentNotifications(true)
                .ocrNotifications(true)
                .paymentNotifications(true)
                .storageNotifications(true)
                .subscriptionNotifications(true)
                .teamNotifications(true)
                .couponNotifications(true)
                .build();

        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .pushEnabled(false)
                .emailEnabled(true)
                .documentNotifications(true)
                .ocrNotifications(true)
                .paymentNotifications(true)
                .storageNotifications(true)
                .subscriptionNotifications(true)
                .teamNotifications(true)
                .couponNotifications(true)
                .build();

        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any(NotificationPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceTokenService.deleteAllDeviceTokens(userId)).thenReturn(2);
        when(sanitizeLogging.sanitizeLogging(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sanitizeLogging.sanitizeLoggingObject(any(Object.class)))
                .thenAnswer(invocation -> {
                    Object value = invocation.getArgument(0, Object.class);
                    return String.valueOf(value);
                });

        service.updatePreferences(userId, request);

        verify(deviceTokenService).deleteAllDeviceTokens(userId);
        verify(preferencesRepository).save(any(NotificationPreferences.class));
    }

    @Test
    void updatePreferences_whenPushAlreadyDisabled_doesNotDeleteTokens() {
        String userId = "user-1";
        NotificationPreferences existing = NotificationPreferences.builder()
                .pushEnabled(false)
                .emailEnabled(true)
                .documentNotifications(true)
                .ocrNotifications(true)
                .paymentNotifications(true)
                .storageNotifications(true)
                .subscriptionNotifications(true)
                .teamNotifications(true)
                .couponNotifications(true)
                .build();

        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .pushEnabled(false)
                .emailEnabled(true)
                .documentNotifications(true)
                .ocrNotifications(true)
                .paymentNotifications(true)
                .storageNotifications(true)
                .subscriptionNotifications(true)
                .teamNotifications(true)
                .couponNotifications(true)
                .build();

        when(preferencesRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any(NotificationPreferences.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(sanitizeLogging.sanitizeLogging(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updatePreferences(userId, request);

        verify(deviceTokenService, never()).deleteAllDeviceTokens(anyString());
    }
}

