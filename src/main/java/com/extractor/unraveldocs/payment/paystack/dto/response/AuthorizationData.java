package com.extractor.unraveldocs.payment.paystack.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authorization data from Paystack
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationData {

    @JsonProperty("authorization_code")
    private String authorizationCode;

    private String bin;

    private String last4;

    @JsonProperty("exp_month")
    private String expMonth;

    @JsonProperty("exp_year")
    private String expYear;

    private String channel;

    @JsonProperty("card_type")
    private String cardType;

    private String bank;

    @JsonProperty("country_code")
    private String countryCode;

    private String brand;

    private boolean reusable;

    private String signature;

    @JsonProperty("account_name")
    private String accountName;

    @JsonProperty("receiver_bank_account_number")
    private String receiverBankAccountNumber;

    @JsonProperty("receiver_bank")
    private String receiverBank;
}

