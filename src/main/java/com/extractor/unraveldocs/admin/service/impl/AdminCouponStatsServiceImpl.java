package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.CouponStatsDto;
import com.extractor.unraveldocs.admin.interfaces.AdminCouponStatsService;
import com.extractor.unraveldocs.coupon.repository.CouponAnalyticsRepository;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.repository.CouponUsageRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCouponStatsServiceImpl implements AdminCouponStatsService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponAnalyticsRepository couponAnalyticsRepository;
    private final ResponseBuilderService responseBuilderService;

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<CouponStatsDto> getCouponStats() {
        log.info("Fetching admin coupon statistics");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime next7Days = now.plusDays(7);

        CouponStatsDto stats = CouponStatsDto.builder()
                .totalCoupons(couponRepository.count())
                .activeCoupons(couponRepository.countActiveCoupons(now))
                .expiredCoupons(couponRepository.countExpiredCoupons(now))
                .totalUsages(couponRepository.sumTotalUsages())
                .totalDiscountGiven(couponAnalyticsRepository.sumTotalDiscountAmount())
                .uniqueUsersWhoUsedCoupons(couponUsageRepository.countGlobalUniqueUsers())
                .averageDiscountPercentage(couponRepository.averageDiscountPercentage())
                .couponsNearExpiry(couponRepository.countCouponsNearExpiry(now, next7Days))
                .couponsAtUsageLimit(couponRepository.countCouponsAtUsageLimit())
                .build();

        return responseBuilderService.buildUserResponse(
                stats,
                HttpStatus.OK,
                "Coupon stats retrieved successfully"
        );
    }
}
