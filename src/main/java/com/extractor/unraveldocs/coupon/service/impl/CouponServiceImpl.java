package com.extractor.unraveldocs.coupon.service.impl;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.dto.CouponUsageData;
import com.extractor.unraveldocs.coupon.dto.request.CreateCouponRequest;
import com.extractor.unraveldocs.coupon.dto.request.UpdateCouponRequest;
import com.extractor.unraveldocs.coupon.dto.response.CouponAnalyticsData;
import com.extractor.unraveldocs.coupon.dto.response.CouponListData;
import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import com.extractor.unraveldocs.coupon.helpers.CouponCodeGenerator;
import com.extractor.unraveldocs.coupon.helpers.CouponMapper;
import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.model.CouponRecipient;
import com.extractor.unraveldocs.coupon.model.CouponTemplate;
import com.extractor.unraveldocs.coupon.model.CouponUsage;
import com.extractor.unraveldocs.coupon.repository.CouponAnalyticsRepository;
import com.extractor.unraveldocs.coupon.repository.CouponRecipientRepository;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.repository.CouponTemplateRepository;
import com.extractor.unraveldocs.coupon.repository.CouponUsageRepository;
import com.extractor.unraveldocs.coupon.service.CouponCacheService;
import com.extractor.unraveldocs.coupon.service.CouponNotificationService;
import com.extractor.unraveldocs.coupon.service.CouponService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponRecipientRepository couponRecipientRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final CouponAnalyticsRepository couponAnalyticsRepository;
    private final UserRepository userRepository;
    private final CouponMapper couponMapper;
    private final CouponCodeGenerator codeGenerator;
    private final ResponseBuilderService responseBuilder;
    private final CouponNotificationService couponNotificationService;
    private final CouponCacheService couponCacheService;
    private final SanitizeLogging sanitizer;

    @Override
    @Transactional
    public UnravelDocsResponse<CouponData> createCoupon(CreateCouponRequest request, User createdBy) {
        log.info("Creating coupon by user: {}", sanitizer.sanitizeLogging(createdBy.getEmail()));

        // Validate date range
        if (request.getValidUntil().isBefore(request.getValidFrom())) {
            return responseBuilder.buildUserResponse(
                    null, HttpStatus.BAD_REQUEST, "Valid until date must be after valid from date");
        }

        // Generate or validate code
        String code;
        if (request.getCustomCode() != null && !request.getCustomCode().isBlank()) {
            code = request.getCustomCode().toUpperCase();
            if (couponRepository.existsByCode(code)) {
                return responseBuilder.buildUserResponse(
                        null, HttpStatus.CONFLICT, "Coupon code already exists");
            }
        } else {
            code = generateUniqueCode(null);
        }

        // Get template if provided
        CouponTemplate template = null;
        if (request.getTemplateId() != null) {
            template = couponTemplateRepository.findById(request.getTemplateId()).orElse(null);
        }

        // Build coupon entity
        Coupon coupon = Coupon.builder()
                .code(code)
                .isCustomCode(request.getCustomCode() != null && !request.getCustomCode().isBlank())
                .description(request.getDescription())
                .recipientCategory(request.getRecipientCategory())
                .discountPercentage(request.getDiscountPercentage())
                .minPurchaseAmount(request.getMinPurchaseAmount())
                .maxUsageCount(request.getMaxUsageCount())
                .maxUsagePerUser(request.getMaxUsagePerUser() != null ? request.getMaxUsagePerUser() : 1)
                .currentUsageCount(0)
                .isActive(true)
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .expiryNotificationSent(false)
                .template(template)
                .createdBy(createdBy)
                .build();

        coupon = couponRepository.save(coupon);
        log.info("Coupon created with code: {}", sanitizer.sanitizeLogging(code));

        // Assign specific recipients if category is SPECIFIC_USERS
        if (request.getRecipientCategory() == RecipientCategory.SPECIFIC_USERS &&
                request.getSpecificUserIds() != null && !request.getSpecificUserIds().isEmpty()) {
            assignSpecificRecipients(coupon, request.getSpecificUserIds());
        }

        // Send notifications if requested
        if (Boolean.TRUE.equals(request.getSendNotifications())) {
            couponNotificationService.notifyRecipients(coupon);
        }

        // Cache the coupon
        couponCacheService.cacheCoupon(coupon);

        CouponData couponData = couponMapper.toCouponData(coupon);
        return responseBuilder.buildUserResponse(couponData, HttpStatus.CREATED, "Coupon created successfully");
    }

    @Override
    @Transactional
    public UnravelDocsResponse<CouponData> updateCoupon(String couponId, UpdateCouponRequest request, User updatedBy) {
        log.info("Updating coupon: {} by user: {}",
                sanitizer.sanitizeLogging(couponId),
                sanitizer.sanitizeLogging(updatedBy.getEmail()));

        Coupon coupon = couponRepository.findById(couponId).orElse(null);
        if (coupon == null) {
            return responseBuilder.buildUserResponse(null, HttpStatus.NOT_FOUND, "Coupon not found");
        }

        // Update fields if provided
        if (request.getDescription() != null) {
            coupon.setDescription(request.getDescription());
        }
        if (request.getDiscountPercentage() != null) {
            coupon.setDiscountPercentage(request.getDiscountPercentage());
        }
        if (request.getMinPurchaseAmount() != null) {
            coupon.setMinPurchaseAmount(request.getMinPurchaseAmount());
        }
        if (request.getRecipientCategory() != null) {
            coupon.setRecipientCategory(request.getRecipientCategory());
        }
        if (request.getMaxUsageCount() != null) {
            coupon.setMaxUsageCount(request.getMaxUsageCount());
        }
        if (request.getMaxUsagePerUser() != null) {
            coupon.setMaxUsagePerUser(request.getMaxUsagePerUser());
        }
        if (request.getValidFrom() != null) {
            coupon.setValidFrom(request.getValidFrom());
        }
        if (request.getValidUntil() != null) {
            coupon.setValidUntil(request.getValidUntil());
        }
        if (request.getIsActive() != null) {
            coupon.setActive(request.getIsActive());
        }

        coupon = couponRepository.save(coupon);

        // Invalidate cache
        couponCacheService.invalidateCoupon(coupon.getCode());

        CouponData couponData = couponMapper.toCouponData(coupon);
        return responseBuilder.buildUserResponse(couponData, HttpStatus.OK, "Coupon updated successfully");
    }

    @Override
    @Transactional
    public UnravelDocsResponse<Void> deactivateCoupon(String couponId, User user) {
        log.info("Deactivating coupon: {} by user: {}",
                sanitizer.sanitizeLogging(couponId),
                sanitizer.sanitizeLogging(user.getEmail()));

        Coupon coupon = couponRepository.findById(couponId).orElse(null);
        if (coupon == null) {
            return responseBuilder.buildVoidResponse(HttpStatus.NOT_FOUND, "Coupon not found");
        }

        coupon.setActive(false);
        couponRepository.save(coupon);

        // Invalidate cache
        couponCacheService.invalidateCoupon(coupon.getCode());

        return responseBuilder.buildVoidResponse(HttpStatus.OK, "Coupon deactivated successfully");
    }

    @Override
    public UnravelDocsResponse<CouponData> getCouponById(String couponId) {
        Coupon coupon = couponRepository.findById(couponId).orElse(null);
        if (coupon == null) {
            return responseBuilder.buildUserResponse(null, HttpStatus.NOT_FOUND, "Coupon not found");
        }

        CouponData couponData = couponMapper.toCouponData(coupon);
        return responseBuilder.buildUserResponse(couponData, HttpStatus.OK, "Coupon retrieved successfully");
    }

    @Override
    public UnravelDocsResponse<CouponData> getCouponByCode(String code) {
        // Check cache first
        var cachedCoupon = couponCacheService.getCouponByCode(code);
        if (cachedCoupon.isPresent()) {
            return responseBuilder.buildUserResponse(cachedCoupon.get(), HttpStatus.OK,
                    "Coupon retrieved successfully");
        }

        Coupon coupon = couponRepository.findByCode(code.toUpperCase()).orElse(null);
        if (coupon == null) {
            return responseBuilder.buildUserResponse(null, HttpStatus.NOT_FOUND, "Coupon not found");
        }

        CouponData couponData = couponMapper.toCouponData(coupon);

        // Cache for future requests
        couponCacheService.cacheCouponData(code, couponData);

        return responseBuilder.buildUserResponse(couponData, HttpStatus.OK, "Coupon retrieved successfully");
    }

    @Override
    public UnravelDocsResponse<CouponListData> getAllCoupons(
            int page, int size, Boolean isActive, RecipientCategory recipientCategory) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Coupon> couponPage;

        if (isActive != null && recipientCategory != null) {
            couponPage = couponRepository.findByIsActiveAndRecipientCategory(isActive, recipientCategory, pageable);
        } else if (isActive != null) {
            couponPage = couponRepository.findByIsActive(isActive, pageable);
        } else if (recipientCategory != null) {
            couponPage = couponRepository.findByRecipientCategory(recipientCategory, pageable);
        } else {
            couponPage = couponRepository.findAll(pageable);
        }

        List<CouponData> coupons = couponPage.getContent().stream()
                .map(couponMapper::toCouponData)
                .collect(Collectors.toList());

        CouponListData listData = CouponListData.builder()
                .coupons(coupons)
                .totalElements(couponPage.getTotalElements())
                .totalPages(couponPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .hasNext(couponPage.hasNext())
                .hasPrevious(couponPage.hasPrevious())
                .build();

        return responseBuilder.buildUserResponse(listData, HttpStatus.OK, "Coupons retrieved successfully");
    }

    @Override
    public UnravelDocsResponse<List<CouponUsageData>> getCouponUsage(String couponId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "usedAt"));
        Page<CouponUsage> usagePage = couponUsageRepository.findByCouponId(couponId, pageable);

        List<CouponUsageData> usageData = usagePage.getContent().stream()
                .map(couponMapper::toUsageData)
                .collect(Collectors.toList());

        return responseBuilder.buildUserResponse(usageData, HttpStatus.OK, "Coupon usage retrieved successfully");
    }

    @Override
    public UnravelDocsResponse<CouponAnalyticsData> getCouponAnalytics(
            String couponId, LocalDate startDate, LocalDate endDate) {

        Coupon coupon = couponRepository.findById(couponId).orElse(null);
        if (coupon == null) {
            return responseBuilder.buildUserResponse(null, HttpStatus.NOT_FOUND, "Coupon not found");
        }

        // Get aggregated stats
        BigDecimal totalDiscount = couponUsageRepository.sumDiscountAmountByCouponId(couponId);
        BigDecimal totalOriginal = couponUsageRepository.sumOriginalAmountByCouponId(couponId);
        int uniqueUsers = couponUsageRepository.countUniqueUsersByCouponId(couponId);

        // Calculate revenue impact
        BigDecimal revenueImpact = totalOriginal.subtract(totalDiscount);
        BigDecimal avgDiscount = coupon.getCurrentUsageCount() > 0 ? totalDiscount.divide(
                BigDecimal.valueOf(coupon.getCurrentUsageCount()), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Get time-series data from pre-calculated analytics
        var dailyAnalytics = couponAnalyticsRepository
                .findByCouponIdAndAnalyticsDateBetweenOrderByAnalyticsDateAsc(couponId, startDate, endDate)
                .stream()
                .map(a -> CouponAnalyticsData.DailyAnalytics.builder()
                        .date(a.getAnalyticsDate().toString())
                        .usageCount(a.getUsageCount())
                        .uniqueUsers(a.getUniqueUsersCount())
                        .discountAmount(a.getTotalDiscountAmount())
                        .revenueImpact(a.getTotalOriginalAmount().subtract(a.getTotalDiscountAmount()))
                        .build())
                .collect(Collectors.toList());

        CouponAnalyticsData analyticsData = CouponAnalyticsData.builder()
                .couponId(couponId)
                .couponCode(coupon.getCode())
                .totalUsageCount(coupon.getCurrentUsageCount())
                .uniqueUsersCount(uniqueUsers)
                .totalDiscountAmount(totalDiscount)
                .totalOriginalAmount(totalOriginal)
                .totalFinalAmount(totalOriginal.subtract(totalDiscount))
                .revenueImpact(revenueImpact)
                .averageDiscountPerTransaction(avgDiscount)
                .dailyAnalytics(dailyAnalytics)
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .build();

        return responseBuilder.buildUserResponse(analyticsData, HttpStatus.OK, "Analytics retrieved successfully");
    }

    @Override
    public UnravelDocsResponse<List<CouponData>> getCouponsForUser(String userId) {
        List<CouponData> availableCoupons = new ArrayList<>();

        // Get coupons where user is a specific recipient
        var recipientAssignments = couponRecipientRepository.findActiveRecipientAssignmentsForUser(userId);
        for (var assignment : recipientAssignments) {
            availableCoupons.add(couponMapper.toCouponData(assignment.getCoupon()));
        }

        // TODO: Also get coupons matching user's category (based on subscription, activity, etc.)

        return responseBuilder.buildUserResponse(availableCoupons, HttpStatus.OK, "Available coupons retrieved");
    }

    @Override
    public String generateCouponCode(String prefix) {
        return generateUniqueCode(prefix);
    }

    private String generateUniqueCode(String prefix) {
        String code;
        int attempts = 0;
        do {
            code = codeGenerator.generate(prefix);
            attempts++;
            if (attempts > 10) {
                log.warn("Too many attempts to generate unique code, using timestamp");
                code = (prefix != null ? prefix + "-" : "") + System.currentTimeMillis();
                break;
            }
        } while (couponRepository.existsByCode(code));
        return code;
    }

    private void assignSpecificRecipients(Coupon coupon, List<String> userIds) {
        for (String userId : userIds) {
            userRepository.findById(userId).ifPresent(user -> {
                CouponRecipient recipient = CouponRecipient.builder()
                        .coupon(coupon)
                        .user(user)
                        .build();
                couponRecipientRepository.save(recipient);
            });
        }
    }
}
