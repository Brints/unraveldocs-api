package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.NotificationStatsDto;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.repository.DeviceTokenRepository;
import com.extractor.unraveldocs.pushnotification.repository.NotificationRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNotificationStatsServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @InjectMocks
    private AdminNotificationStatsServiceImpl adminNotificationStatsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getNotificationStats_Success() {
        // Arrange
        when(notificationRepository.count()).thenReturn(1000L);
        when(notificationRepository.countByIsReadTrue()).thenReturn(600L);
        when(notificationRepository.countByIsReadFalse()).thenReturn(400L);
        when(deviceTokenRepository.count()).thenReturn(250L);

        List<Object[]> notifTypesRaw = new ArrayList<>();
        notifTypesRaw.add(new Object[]{NotificationType.OCR_PROCESSING_COMPLETED, 700L});
        notifTypesRaw.add(new Object[]{NotificationType.SYSTEM_ANNOUNCEMENT, 300L});
        when(notificationRepository.countNotificationsByType()).thenReturn(notifTypesRaw);

        List<Object[]> devicesRaw = new ArrayList<>();
        devicesRaw.add(new Object[]{"FCM", 150L});
        devicesRaw.add(new Object[]{"APNS", 100L});
        when(deviceTokenRepository.countDevicesByType()).thenReturn(devicesRaw);

        NotificationStatsDto expectedStats = NotificationStatsDto.builder()
                .totalNotifications(1000L)
                .readCount(600L)
                .unreadCount(400L)
                .readRate(60.0)
                .registeredDevices(250L)
                .notificationsByType(Map.of("OCR_PROCESSING_COMPLETED", 700L, "SYSTEM_ANNOUNCEMENT", 300L))
                .devicesByType(Map.of("FCM", 150L, "APNS", 100L))
                .build();

        UnravelDocsResponse<NotificationStatsDto> expectedResponse = new UnravelDocsResponse<>(
                200,
                "success",
                "Notification stats retrieved successfully",
                expectedStats
        );

        when(responseBuilderService.buildUserResponse(
                any(NotificationStatsDto.class),
                eq(HttpStatus.OK),
                eq("Notification stats retrieved successfully")
        )).thenReturn(expectedResponse);

        // Act
        UnravelDocsResponse<NotificationStatsDto> response = adminNotificationStatsService.getNotificationStats();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("success", response.getStatus());

        NotificationStatsDto data = response.getData();
        assertNotNull(data);
        assertEquals(1000L, data.getTotalNotifications());
        assertEquals(600L, data.getReadCount());
        assertEquals(400L, data.getUnreadCount());
        assertEquals(60.0, data.getReadRate());
        assertEquals(250L, data.getRegisteredDevices());
        assertEquals(700L, data.getNotificationsByType().get("OCR_PROCESSING_COMPLETED"));
        assertEquals(150L, data.getDevicesByType().get("FCM"));
    }
}
