package com.extractor.unraveldocs.payment.paystack.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for constructing the Paystack transaction initialization API request payload.
 * This is sent directly to Paystack's /transaction/initialize endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaystackTransactionPayload {

    private String email;

    /**
     * Amount in the smallest currency unit (e.g., kobo for NGN)
     */
    private Long amount;

    private String reference;

    private String currency;

    @JsonProperty("callback_url")
    private String callbackUrl;

    /**
     * Paystack plan code for subscription transactions (starts with PLN_)
     */
    private String plan;

    private String[] channels;

    private TransactionMetadata metadata;
}
