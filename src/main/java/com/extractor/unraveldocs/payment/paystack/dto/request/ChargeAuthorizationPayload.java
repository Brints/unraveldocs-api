package com.extractor.unraveldocs.payment.paystack.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for constructing the Paystack charge authorization API request payload.
 * This is sent directly to Paystack's /transaction/charge_authorization endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChargeAuthorizationPayload {

    private String email;

    /**
     * Amount in the smallest currency unit (e.g., kobo for NGN)
     */
    private Long amount;

    @JsonProperty("authorization_code")
    private String authorizationCode;

    private String reference;

    private String currency;

    private TransactionMetadata metadata;
}
