package com.extractor.unraveldocs.payment.paystack.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transaction metadata sent to Paystack.
 * Contains user-specific and transaction-specific information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionMetadata {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("customer_code")
    private String customerCode;

    /**
     * Internal plan code for subscription tracking after payment
     */
    @JsonProperty("plan_code")
    private String planCode;

    /**
     * Applied coupon code, if any
     */
    @JsonProperty("coupon_code")
    private String couponCode;

    /**
     * Original amount before discount (as string for consistent serialization)
     */
    @JsonProperty("original_amount")
    private String originalAmount;

    /**
     * Discount amount applied (as string for consistent serialization)
     */
    @JsonProperty("discount_amount")
    private String discountAmount;
}
