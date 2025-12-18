package com.extractor.unraveldocs.payment.paystack.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a Paystack subscription plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanRequest {

    @NotBlank(message = "Plan name is required")
    private String name;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount; // Amount in kobo

    @NotBlank(message = "Interval is required")
    private String interval; // daily, weekly, monthly, quarterly, biannually, annually

    private String description;

    private String currency;

    @JsonProperty("invoice_limit")
    private Integer invoiceLimit; // Number of times to charge customer before stopping

    @JsonProperty("send_invoices")
    private Boolean sendInvoices;

    @JsonProperty("send_sms")
    private Boolean sendSms;
}

