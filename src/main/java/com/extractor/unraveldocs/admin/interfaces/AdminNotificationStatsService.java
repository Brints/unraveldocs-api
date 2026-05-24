package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.response.NotificationStatsDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminNotificationStatsService {
    UnravelDocsResponse<NotificationStatsDto> getNotificationStats();
}
