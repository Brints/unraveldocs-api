package com.extractor.unraveldocs.payment.paystack.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Paystack subscription data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionData {

    private Long id;

    private String domain;

    private String status;

    @JsonProperty("subscription_code")
    private String subscriptionCode;

    @JsonProperty("email_token")
    private String emailToken;

    private Long amount;

    @JsonProperty("cron_expression")
    private String cronExpression;

    @JsonProperty("next_payment_date")
    private String nextPaymentDate;

    @JsonProperty("open_invoice")
    private String openInvoice;

    @JsonProperty("invoice_limit")
    private Integer invoiceLimit;

    @JsonProperty("payments_count")
    private Integer paymentsCount;

    @JsonProperty("most_recent_invoice")
    private Object mostRecentInvoice;

    private PlanData plan;

    private CustomerData customer;

    private AuthorizationData authorization;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("cancelled_at")
    private String cancelledAt;
}

