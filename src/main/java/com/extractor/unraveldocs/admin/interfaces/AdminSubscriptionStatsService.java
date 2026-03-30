package com.extractor.unraveldocs.admin.interfaces;

import com.extractor.unraveldocs.admin.dto.response.SubscriptionStatsDto;

public interface AdminSubscriptionStatsService {

    /**
     * Retrieves aggregated subscription and revenue statistics for the admin dashboard.
     *
     * @return SubscriptionStatsDto containing breakdowns by plan, status, source, MRR, churn, and quota alerts.
     */
    SubscriptionStatsDto getSubscriptionStatistics();
}
