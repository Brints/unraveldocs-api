package com.extractor.unraveldocs.payment.paystack.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for transaction verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionData {

    private Long id;

    private String domain;

    private String status;

    private String reference;

    private Long amount;

    private String message;

    @JsonProperty("gateway_response")
    private String gatewayResponse;

    @JsonProperty("paid_at")
    private String paidAt;

    @JsonProperty("created_at")
    private String createdAt;

    private String channel;

    private String currency;

    @JsonProperty("ip_address")
    private String ipAddress;

    private Map<String, Object> metadata;

    private CustomerData customer;

    private AuthorizationData authorization;

    @JsonProperty("plan_object")
    private PlanData plan;

    @JsonProperty("fees")
    private Long fees;

    @JsonProperty("fees_split")
    private Object feesSplit;

    @JsonProperty("requested_amount")
    private Long requestedAmount;

    @JsonProperty("transaction_date")
    private String transactionDate;
}

