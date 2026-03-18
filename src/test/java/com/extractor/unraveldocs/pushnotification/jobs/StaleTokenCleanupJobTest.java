package com.extractor.unraveldocs.pushnotification.jobs;

import com.extractor.unraveldocs.pushnotification.config.NotificationConfig;
import com.extractor.unraveldocs.pushnotification.repository.DeviceTokenRepository;
import com.extractor.unraveldocs.pushnotification.repository.NotificationPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaleTokenCleanupJobTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private NotificationPreferencesRepository notificationPreferencesRepository;

    @Mock
    private NotificationConfig notificationConfig;

    @InjectMocks
    private StaleTokenCleanupJob job;

    @BeforeEach
    void setUp() {
        when(notificationConfig.getTokenRetentionDays()).thenReturn(30);
    }

    @Test
    void cleanupStaleTokens_runsAllCleanupPaths() {
        when(deviceTokenRepository.deleteInactiveOlderThan(any())).thenReturn(3);
        when(deviceTokenRepository.deactivateStaleActiveTokens(any())).thenReturn(4);
        when(notificationPreferencesRepository.findUserIdsWithPushDisabled()).thenReturn(List.of("u1", "u2"));
        when(deviceTokenRepository.deleteAllByUserId("u1")).thenReturn(2);
        when(deviceTokenRepository.deleteAllByUserId("u2")).thenReturn(1);

        job.cleanupStaleTokens();

        verify(deviceTokenRepository).deleteInactiveOlderThan(any());
        verify(deviceTokenRepository).deactivateStaleActiveTokens(any());
        verify(notificationPreferencesRepository).findUserIdsWithPushDisabled();
        verify(deviceTokenRepository).deleteAllByUserId("u1");
        verify(deviceTokenRepository).deleteAllByUserId("u2");
    }
}

