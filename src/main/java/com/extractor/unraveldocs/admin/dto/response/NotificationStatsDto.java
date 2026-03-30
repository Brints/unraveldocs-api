package com.extractor.unraveldocs.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDto {
    private long totalNotifications;
    private Map<String, Long> notificationsByType;
    private long readCount;
    private long unreadCount;
    private double readRate;
    private long registeredDevices;
    private Map<String, Long> devicesByType;
}
