package com.extractor.unraveldocs.payment.paystack.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Request DTO for creating a Paystack subscription
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotBlank(message = "Customer email or code is required")
    private String customer;

    @JsonProperty("plan_name")
    @NotBlank(message = "Plan name is required")
    private String planName;

    private String authorization;

    @JsonProperty("start_date")
    private OffsetDateTime startDate;
}

