package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.response.PaymentStatsDto;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;

public interface AdminPaymentStatsService {
    
    /**
     * Get aggregated payment and revenue statistics for the admin dashboard.
     * @return UnravelDocsResponse containing the PaymentStatsDto
     */
    UnravelDocsResponse<PaymentStatsDto> getPaymentStatistics();
}
