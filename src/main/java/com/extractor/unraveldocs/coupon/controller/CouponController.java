package com.extractor.unraveldocs.coupon.controller;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.dto.request.ApplyCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.CouponValidationResponse;
import com.extractor.unraveldocs.coupon.dto.response.DiscountCalculationData;
import com.extractor.unraveldocs.coupon.service.CouponService;
import com.extractor.unraveldocs.coupon.service.CouponValidationService;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Client-facing controller for coupon validation and application.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponValidationService couponValidationService;
    private final ResponseBuilderService responseBuilder;

    /**
     * Validates a coupon code for the current user.
     */
    @GetMapping("/validate/{code}")
    public ResponseEntity<UnravelDocsResponse<CouponValidationResponse>> validateCoupon(
            @PathVariable String code,
            @AuthenticationPrincipal User user) {
        log.info("User {} validating coupon: {}", user.getEmail(), code);

        CouponValidationResponse validation = couponValidationService.validateCoupon(code, user);

        UnravelDocsResponse<CouponValidationResponse> response = responseBuilder.buildUserResponse(
                validation,
                validation.isValid() ? HttpStatus.OK : HttpStatus.BAD_REQUEST,
                validation.getMessage());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Applies a coupon to an amount and calculates the discount.
     */
    @PostMapping("/apply")
    public ResponseEntity<UnravelDocsResponse<DiscountCalculationData>> applyCoupon(
            @Valid @RequestBody ApplyCouponRequest request,
            @AuthenticationPrincipal User user) {
        log.info("User {} applying coupon: {} to amount: {}",
                user.getEmail(), request.getCouponCode(), request.getAmount());

        DiscountCalculationData discountData = couponValidationService.applyCouponToAmount(request, user);

        if (discountData == null) {
            UnravelDocsResponse<DiscountCalculationData> response = responseBuilder.buildUserResponse(
                    null, HttpStatus.BAD_REQUEST, "Invalid coupon");
            return ResponseEntity.status(response.getStatusCode()).body(response);
        }

        if (!discountData.isMinPurchaseRequirementMet()) {
            String message = String.format("Minimum purchase amount of %.2f required",
                    discountData.getMinPurchaseAmount());
            UnravelDocsResponse<DiscountCalculationData> response = responseBuilder.buildUserResponse(
                    discountData, HttpStatus.BAD_REQUEST, message);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        }

        UnravelDocsResponse<DiscountCalculationData> response = responseBuilder.buildUserResponse(
                discountData, HttpStatus.OK, "Coupon applied successfully");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Gets coupon details by code.
     */
    @GetMapping("/{code}")
    public ResponseEntity<UnravelDocsResponse<CouponData>> getCouponByCode(@PathVariable String code) {
        UnravelDocsResponse<CouponData> response = couponService.getCouponByCode(code);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Gets all coupons available for the current user.
     */
    @GetMapping("/available")
    public ResponseEntity<UnravelDocsResponse<List<CouponData>>> getAvailableCoupons(
            @AuthenticationPrincipal User user) {
        log.info("User {} fetching available coupons", user.getEmail());
        UnravelDocsResponse<List<CouponData>> response = couponService.getCouponsForUser(user.getId());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
