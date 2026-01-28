package com.extractor.unraveldocs.pushnotification.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Request DTO for updating notification preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferencesRequest {

    @NotNull(message = "Push enabled flag is required")
    private Boolean pushEnabled;

    @NotNull(message = "Email enabled flag is required")
    private Boolean emailEnabled;

    @NotNull(message = "Document notifications flag is required")
    private Boolean documentNotifications;

    @NotNull(message = "OCR notifications flag is required")
    private Boolean ocrNotifications;

    @NotNull(message = "Payment notifications flag is required")
    private Boolean paymentNotifications;

    @NotNull(message = "Storage notifications flag is required")
    private Boolean storageNotifications;

    @NotNull(message = "Subscription notifications flag is required")
    private Boolean subscriptionNotifications;

    @NotNull(message = "Team notifications flag is required")
    private Boolean teamNotifications;

    private Boolean couponNotifications = Boolean.TRUE;

    private Boolean quietHoursEnabled;

    private LocalTime quietHoursStart;

    private LocalTime quietHoursEnd;
}
