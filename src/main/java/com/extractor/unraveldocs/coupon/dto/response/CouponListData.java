package com.extractor.unraveldocs.coupon.dto.response;

import com.extractor.unraveldocs.coupon.dto.CouponData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response DTO for coupon lists.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponListData {
    private List<CouponData> coupons;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
