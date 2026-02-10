package com.extractor.unraveldocs.payment.paystack.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for initializing a Paystack transaction
 * 
 * <p>
 * Note: The amount should be provided in the smallest currency unit (e.g., kobo
 * for NGN, cents for USD).
 * For example, to charge ₦13,950.00, send amount as 1395000 (kobo).
 * This matches Paystack's expected format directly.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeTransactionRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * Amount in the smallest currency unit (e.g., kobo for NGN).
     * For ₦13,950.00, send 1395000
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @JsonProperty("callback_url")
    private String callbackUrl;

    private String reference;

    private String currency;

    @JsonProperty("planCode")
    @com.fasterxml.jackson.annotation.JsonAlias({ "plan", "plan_code" })
    private String planCode;

    @JsonProperty("subscription_start_date")
    private String subscriptionStartDate;

    private String[] channels;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("subaccount")
    private String subaccount;

    @JsonProperty("split_code")
    private String splitCode;

    @JsonProperty("bearer")
    private String bearer;

    /**
     * Optional coupon code to apply discount to this transaction.
     * If provided, the coupon will be validated and the discounted amount
     * will be sent to Paystack.
     */
    @JsonProperty("coupon_code")
    private String couponCode;
}
