package com.extractor.unraveldocs.coupon.service.impl;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import com.extractor.unraveldocs.coupon.helpers.CouponMapper;
import com.extractor.unraveldocs.coupon.model.Coupon;
import com.extractor.unraveldocs.coupon.repository.CouponRepository;
import com.extractor.unraveldocs.coupon.service.CouponCacheService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponCacheServiceImpl implements CouponCacheService {

    private static final String CACHE_PREFIX = "coupon:code:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final int WARM_CACHE_SIZE = 100;

    private final RedisTemplate<String, String> redisTemplate;
    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;
    private final ObjectMapper objectMapper;

    // Simple stats tracking
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);

    private final SanitizeLogging sanitizer;

    @PostConstruct
    public void init() {
        // Warm cache on startup
        warmCache();
    }

    @Override
    public Optional<CouponData> getCouponByCode(String code) {
        String key = CACHE_PREFIX + code.toUpperCase();
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                hits.incrementAndGet();
                return Optional.of(objectMapper.readValue(json, CouponData.class));
            }
            misses.incrementAndGet();
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.error("Error deserializing cached coupon: {}",
                    sanitizer.sanitizeLogging(code), e);
            misses.incrementAndGet();
            return Optional.empty();
        }
    }

    @Override
    public void cacheCoupon(Coupon coupon) {
        CouponData data = couponMapper.toCouponData(coupon);
        cacheCouponData(coupon.getCode(), data);
    }

    @Override
    public void cacheCouponData(String code, CouponData couponData) {
        String key = CACHE_PREFIX + code.toUpperCase();
        try {
            String json = objectMapper.writeValueAsString(couponData);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
            log.debug("Cached coupon: {}", sanitizer.sanitizeLogging(code));
        } catch (JsonProcessingException e) {
            log.error("Error serializing coupon for cache: {}",
                    sanitizer.sanitizeLogging(code), e);
        }
    }

    @Override
    public void invalidateCoupon(String code) {
        String key = CACHE_PREFIX + code.toUpperCase();
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            evictions.incrementAndGet();
            log.debug("Invalidated cached coupon: {}", sanitizer.sanitizeLogging(code));
        }
    }

    @Override
    public void invalidateAll() {
        var keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            Long deleted = redisTemplate.delete(keys);
            evictions.addAndGet(deleted != null ? deleted : 0);
            log.info("Invalidated {} cached coupons",
                    sanitizer.sanitizeLoggingObject(deleted));
        }
    }

    @Override
    public void warmCache() {
        log.info("Warming coupon cache...");
        try {
            OffsetDateTime now = OffsetDateTime.now();
            var validCoupons = couponRepository.findAllValidCoupons(now, PageRequest.of(0, WARM_CACHE_SIZE));

            for (Coupon coupon : validCoupons) {
                cacheCoupon(coupon);
            }

            log.info("Cached {} valid coupons",
                    sanitizer.sanitizeLoggingInteger(validCoupons.size()));
        } catch (Exception e) {
            log.warn("Failed to warm coupon cache: {}", e.getMessage());
        }
    }

    @Override
    public boolean isCached(String code) {
        String key = CACHE_PREFIX + code.toUpperCase();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public CacheStats getCacheStats() {
        long size = 0;
        var keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null) {
            size = keys.size();
        }
        return new CacheStats(hits.get(), misses.get(), evictions.get(), size);
    }
}
