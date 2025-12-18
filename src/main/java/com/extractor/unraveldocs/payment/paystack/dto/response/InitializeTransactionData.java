package com.extractor.unraveldocs.payment.paystack.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for transaction initialization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitializeTransactionData {

    @JsonProperty("authorization_url")
    private String authorizationUrl;

    @JsonProperty("access_code")
    private String accessCode;

    private String reference;
}

