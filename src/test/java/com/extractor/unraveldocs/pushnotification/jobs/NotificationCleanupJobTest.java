package com.extractor.unraveldocs.pushnotification.jobs;

import com.extractor.unraveldocs.pushnotification.config.NotificationConfig;
import com.extractor.unraveldocs.pushnotification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupJobTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationConfig notificationConfig;

    @InjectMocks
    private NotificationCleanupJob job;

    @Test
    void cleanupOldNotifications_deletesUsingConfiguredRetention() {
        when(notificationConfig.getNotificationRetentionDays()).thenReturn(90);
        when(notificationRepository.deleteOlderThan(any())).thenReturn(5);

        job.cleanupOldNotifications();

        verify(notificationRepository).deleteOlderThan(any());
    }
}

