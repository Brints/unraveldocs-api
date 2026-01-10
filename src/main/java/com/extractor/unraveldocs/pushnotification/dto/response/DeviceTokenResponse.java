package com.extractor.unraveldocs.pushnotification.dto.response;

import com.extractor.unraveldocs.pushnotification.datamodel.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for device token data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenResponse {

    private String id;
    private String deviceToken;
    private DeviceType deviceType;
    private String deviceName;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastUsedAt;
}
