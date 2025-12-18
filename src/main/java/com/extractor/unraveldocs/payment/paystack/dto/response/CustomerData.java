package com.extractor.unraveldocs.payment.paystack.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for customer data from Paystack
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerData {

    private Long id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String email;

    @JsonProperty("customer_code")
    private String customerCode;

    private String phone;

    private Object metadata;

    @JsonProperty("risk_action")
    private String riskAction;

    @JsonProperty("international_format_phone")
    private String internationalFormatPhone;
}

