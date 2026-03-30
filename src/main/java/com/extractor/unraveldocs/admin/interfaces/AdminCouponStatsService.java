package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.response.CouponStatsDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminCouponStatsService {
    UnravelDocsResponse<CouponStatsDto> getCouponStats();
}
