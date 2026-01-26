package com.extractor.unraveldocs.coupon.repository;

import com.extractor.unraveldocs.coupon.model.CouponTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, String> {

    Optional<CouponTemplate> findByName(String name);

    boolean existsByName(String name);

    List<CouponTemplate> findByCreatedById(String userId);

    Page<CouponTemplate> findByIsActive(boolean isActive, Pageable pageable);

    Page<CouponTemplate> findByCreatedByIdAndIsActive(String userId, boolean isActive, Pageable pageable);
}
