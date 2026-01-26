package com.extractor.unraveldocs.coupon.service;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.model.Coupon;

import java.util.Optional;

/**
 * Service interface for Redis caching of coupons.
 */
public interface CouponCacheService {

    /**
     * Gets a coupon from cache by code. Returns empty if not cached.
     */
    Optional<CouponData> getCouponByCode(String code);

    /**
     * Caches a coupon.
     */
    void cacheCoupon(Coupon coupon);

    /**
     * Caches a coupon data DTO.
     */
    void cacheCouponData(String code, CouponData couponData);

    /**
     * Invalidates a coupon from cache.
     */
    void invalidateCoupon(String code);

    /**
     * Invalidates all cached coupons.
     */
    void invalidateAll();

    /**
     * Warms the cache with all currently valid coupons.
     */
    void warmCache();

    /**
     * Checks if a coupon is cached.
     */
    boolean isCached(String code);

    /**
     * Gets cache statistics.
     */
    CacheStats getCacheStats();

    /**
     * Cache statistics.
     */
    record CacheStats(long hits, long misses, long evictions, long size) {
    }
}
