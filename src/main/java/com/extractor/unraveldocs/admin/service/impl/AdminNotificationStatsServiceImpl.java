package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.NotificationStatsDto;
import com.extractor.unraveldocs.admin.interfaces.AdminNotificationStatsService;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.repository.DeviceTokenRepository;
import com.extractor.unraveldocs.pushnotification.repository.NotificationRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationStatsServiceImpl implements AdminNotificationStatsService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final ResponseBuilderService responseBuilderService;

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<NotificationStatsDto> getNotificationStats() {
        log.info("Fetching admin notification statistics");

        long totalNotifications = notificationRepository.count();
        long readCount = notificationRepository.countByIsReadTrue();
        long unreadCount = notificationRepository.countByIsReadFalse();
        double readRate = totalNotifications == 0 ? 0.0 : ((double) readCount / totalNotifications) * 100.0;
        
        long registeredDevices = deviceTokenRepository.count();

        List<Object[]> notificationsByTypeRaw = notificationRepository.countNotificationsByType();
        Map<String, Long> notificationsByType = new LinkedHashMap<>();
        for (Object[] row : notificationsByTypeRaw) {
            String type = ((NotificationType) row[0]).name();
            Long count = ((Number) row[1]).longValue();
            notificationsByType.put(type, count);
        }

        List<Object[]> devicesByTypeRaw = deviceTokenRepository.countDevicesByType();
        Map<String, Long> devicesByType = new LinkedHashMap<>();
        for (Object[] row : devicesByTypeRaw) {
            String type = String.valueOf(row[0]);
            Long count = ((Number) row[1]).longValue();
            devicesByType.put(type, count);
        }

        NotificationStatsDto stats = NotificationStatsDto.builder()
                .totalNotifications(totalNotifications)
                .notificationsByType(notificationsByType)
                .readCount(readCount)
                .unreadCount(unreadCount)
                .readRate(readRate)
                .registeredDevices(registeredDevices)
                .devicesByType(devicesByType)
                .build();

        return responseBuilderService.buildUserResponse(
                stats,
                HttpStatus.OK,
                "Notification stats retrieved successfully"
        );
    }
}
