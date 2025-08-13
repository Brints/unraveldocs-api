package com.extractor.unraveldocs.payment.stripe.dto.response;

import lombok.Data;

@Data
public class CheckoutResponseDto {
    private String sessionId;
    private String sessionUrl;
}
