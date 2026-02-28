package com.extractor.unraveldocs.credit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for purchasing a credit pack.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseCreditPackRequest {

    @NotBlank(message = "Credit pack ID is required")
    private String creditPackId;

    @NotBlank(message = "Payment gateway is required")
    private String gateway;

    @NotBlank(message = "Callback URL is required")
    private String callbackUrl;

    private String cancelUrl;

    /**
     * Optional coupon code for discount.
     */
    private String couponCode;

    /**
     * Optional payment currency (e.g., "NGN", "EUR"). Defaults to USD if not
     * provided.
     * The amount will be converted from USD to this currency before sending to the
     * gateway.
     */
    private String currency;
}
