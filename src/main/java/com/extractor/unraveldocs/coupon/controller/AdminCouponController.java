package com.extractor.unraveldocs.coupon.controller;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.dto.CouponUsageData;
import com.extractor.unraveldocs.coupon.dto.request.BulkCouponGenerationRequest;
import com.extractor.unraveldocs.coupon.dto.request.CreateCouponRequest;
import com.extractor.unraveldocs.coupon.dto.request.UpdateCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.BulkGenerationJobResponse;
import com.extractor.unraveldocs.coupon.dto.response.CouponAnalyticsData;
import com.extractor.unraveldocs.coupon.dto.response.CouponListData;
import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import com.extractor.unraveldocs.coupon.service.BulkCouponGenerationService;
import com.extractor.unraveldocs.coupon.service.CouponService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin controller for coupon management operations.
 * Requires ADMIN or SUPER_ADMIN role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/coupons")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;
    private final BulkCouponGenerationService bulkCouponGenerationService;

    @Autowired
    public AdminCouponController(
            CouponService couponService,
            @Autowired(required = false) BulkCouponGenerationService bulkCouponGenerationService) {
        this.couponService = couponService;
        this.bulkCouponGenerationService = bulkCouponGenerationService;
    }

    /**
     * Creates a new coupon.
     */
    @PostMapping
    public ResponseEntity<UnravelDocsResponse<CouponData>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Admin {} creating coupon", user.getEmail());
        UnravelDocsResponse<CouponData> response = couponService.createCoupon(request, user);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Updates an existing coupon.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UnravelDocsResponse<CouponData>> updateCoupon(
            @PathVariable String id,
            @Valid @RequestBody UpdateCouponRequest request,
            @AuthenticationPrincipal User user) {
        log.info("Admin {} updating coupon: {}", user.getEmail(), id);
        UnravelDocsResponse<CouponData> response = couponService.updateCoupon(id, request, user);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Deactivates a coupon.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<UnravelDocsResponse<Void>> deactivateCoupon(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        log.info("Admin {} deactivating coupon: {}", user.getEmail(), id);
        UnravelDocsResponse<Void> response = couponService.deactivateCoupon(id, user);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Gets a coupon by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UnravelDocsResponse<CouponData>> getCouponById(@PathVariable String id) {
        UnravelDocsResponse<CouponData> response = couponService.getCouponById(id);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Gets all coupons with optional filtering.
     */
    @GetMapping
    public ResponseEntity<UnravelDocsResponse<CouponListData>> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) RecipientCategory recipientCategory) {
        UnravelDocsResponse<CouponListData> response = couponService.getAllCoupons(
                page, size, isActive, recipientCategory);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Gets usage details for a coupon.
     */
    @GetMapping("/{id}/usage")
    public ResponseEntity<UnravelDocsResponse<List<CouponUsageData>>> getCouponUsage(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UnravelDocsResponse<List<CouponUsageData>> response = couponService.getCouponUsage(id, page, size);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Gets analytics for a coupon.
     */
    @GetMapping("/{id}/analytics")
    public ResponseEntity<UnravelDocsResponse<CouponAnalyticsData>> getCouponAnalytics(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UnravelDocsResponse<CouponAnalyticsData> response = couponService.getCouponAnalytics(id, startDate, endDate);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    // ========== Bulk Generation Endpoints ==========

    /**
     * Initiates bulk coupon generation via Kafka.
     * Returns 503 Service Unavailable if Kafka is not configured.
     */
    @PostMapping("/bulk-generate")
    public ResponseEntity<UnravelDocsResponse<BulkGenerationJobResponse>> bulkGenerate(
            @Valid @RequestBody BulkCouponGenerationRequest request,
            @AuthenticationPrincipal User user) {
        if (bulkCouponGenerationService == null) {
            log.warn("Bulk generation requested but Kafka is not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new UnravelDocsResponse<>(
                            503,
                            HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                            "Bulk coupon generation is not available. Kafka is not configured.",
                            null));
        }
        log.info("Admin {} initiating bulk generation of {} coupons", user.getEmail(), request.getQuantity());
        UnravelDocsResponse<BulkGenerationJobResponse> response = bulkCouponGenerationService
                .generateBulkCoupons(request, user);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Gets the status of a bulk generation job.
     * Returns 503 Service Unavailable if Kafka is not configured.
     */
    @GetMapping("/bulk-jobs/{jobId}")
    public ResponseEntity<UnravelDocsResponse<BulkGenerationJobResponse>> getBulkJobStatus(
            @PathVariable String jobId) {
        if (bulkCouponGenerationService == null) {
            log.warn("Bulk job status requested but Kafka is not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new UnravelDocsResponse<>(
                            503,
                            HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                            "Bulk coupon generation is not available. Kafka is not configured.",
                            null));
        }
        UnravelDocsResponse<BulkGenerationJobResponse> response = bulkCouponGenerationService.getJobStatus(jobId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
