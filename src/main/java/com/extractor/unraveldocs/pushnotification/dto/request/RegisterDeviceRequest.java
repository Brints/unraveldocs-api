package com.extractor.unraveldocs.pushnotification.dto.request;

import com.extractor.unraveldocs.pushnotification.datamodel.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering a device token for push notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDeviceRequest {

    @NotBlank(message = "Device token is required")
    @Size(max = 512, message = "Device token must not exceed 512 characters")
    private String deviceToken;

    @NotNull(message = "Device type is required")
    private DeviceType deviceType;

    @Size(max = 100, message = "Device name must not exceed 100 characters")
    private String deviceName;
}
