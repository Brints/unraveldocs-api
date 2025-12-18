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

import java.util.Map;

/**
 * Request DTO for initializing a Paystack transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeTransactionRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    @JsonProperty("callback_url")
    private String callbackUrl;

    private String reference;

    private String currency;

    @JsonProperty("plan")
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
}

