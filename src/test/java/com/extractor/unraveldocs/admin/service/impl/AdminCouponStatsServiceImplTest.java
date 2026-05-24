package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.CouponStatsDto;
import com.extractor.unraveldocs.coupon.repository.CouponAnalyticsRepository;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.repository.CouponUsageRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCouponStatsServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private CouponAnalyticsRepository couponAnalyticsRepository;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @InjectMocks
    private AdminCouponStatsServiceImpl adminCouponStatsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getCouponStats_Success() {
        // Arrange
        when(couponRepository.count()).thenReturn(100L);
        when(couponRepository.countActiveCoupons(any(OffsetDateTime.class))).thenReturn(60L);
        when(couponRepository.countExpiredCoupons(any(OffsetDateTime.class))).thenReturn(40L);
        when(couponRepository.sumTotalUsages()).thenReturn(250L);
        when(couponAnalyticsRepository.sumTotalDiscountAmount()).thenReturn(new BigDecimal("1500.00"));
        when(couponUsageRepository.countGlobalUniqueUsers()).thenReturn(120L);
        when(couponRepository.averageDiscountPercentage()).thenReturn(new BigDecimal("15.5"));
        when(couponRepository.countCouponsNearExpiry(any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(5L);
        when(couponRepository.countCouponsAtUsageLimit()).thenReturn(10L);

        CouponStatsDto expectedStats = CouponStatsDto.builder()
                .totalCoupons(100L)
                .activeCoupons(60L)
                .expiredCoupons(40L)
                .totalUsages(250L)
                .totalDiscountGiven(new BigDecimal("1500.00"))
                .uniqueUsersWhoUsedCoupons(120L)
                .averageDiscountPercentage(new BigDecimal("15.5"))
                .couponsNearExpiry(5L)
                .couponsAtUsageLimit(10L)
                .build();

        UnravelDocsResponse<CouponStatsDto> expectedResponse = new UnravelDocsResponse<>(
                200,
                "success",
                "Coupon stats retrieved successfully",
                expectedStats
        );

        when(responseBuilderService.buildUserResponse(
                eq(expectedStats),
                eq(org.springframework.http.HttpStatus.OK),
                eq("Coupon stats retrieved successfully")
        )).thenReturn(expectedResponse);

        // Act
        UnravelDocsResponse<CouponStatsDto> response = adminCouponStatsService.getCouponStats();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("success", response.getStatus());

        CouponStatsDto data = response.getData();
        assertNotNull(data);
        assertEquals(100L, data.getTotalCoupons());
        assertEquals(60L, data.getActiveCoupons());
        assertEquals(40L, data.getExpiredCoupons());
        assertEquals(250L, data.getTotalUsages());
        assertEquals(new BigDecimal("1500.00"), data.getTotalDiscountGiven());
        assertEquals(120L, data.getUniqueUsersWhoUsedCoupons());
        assertEquals(new BigDecimal("15.5"), data.getAverageDiscountPercentage());
        assertEquals(5L, data.getCouponsNearExpiry());
        assertEquals(10L, data.getCouponsAtUsageLimit());
    }
}
