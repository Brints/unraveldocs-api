package com.extractor.unraveldocs.coupon.dto.response;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for coupon validation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponValidationResponse {
    private boolean isValid;
    private String message;
    private CouponData couponData;

    /**
     * Error code for programmatic handling.
     */
    private String errorCode;

    public static CouponValidationResponse valid(CouponData couponData) {
        return CouponValidationResponse.builder()
                .isValid(true)
                .message("Coupon is valid")
                .couponData(couponData)
                .build();
    }

    public static CouponValidationResponse invalid(String message, String errorCode) {
        return CouponValidationResponse.builder()
                .isValid(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
