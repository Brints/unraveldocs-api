package com.extractor.unraveldocs.subscription.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for user subscription details response.
 * Provides complete subscription information including plan details,
 * billing periods, trial status, and usage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSubscriptionDetailsDto {

    // Subscription identity
    private String subscriptionId;
    private String status; // active, trialing, canceled, expired, etc.

    // Plan information
    private String planId;
    private String planName;
    private String planDisplayName;
    private BigDecimal planPrice;
    private String currency;
    private String billingInterval; // e.g., "monthly", "yearly"

    // Billing period
    private OffsetDateTime currentPeriodStart;
    private OffsetDateTime currentPeriodEnd;
    private Boolean autoRenew;

    // Trial information
    private Boolean isOnTrial;
    private OffsetDateTime trialEndsAt;
    private Boolean hasUsedTrial;
    private Integer trialDaysRemaining;

    // Usage limits
    private Long storageLimit;
    private Long storageUsed;
    private Integer documentUploadLimit;
    private Integer documentsUploaded;
    private Integer ocrPageLimit;
    private Integer ocrPagesUsed;

    // Payment gateway info
    private String paymentGatewaySubscriptionId;

    // Metadata
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
