package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.SubscriptionStatsDto;
import com.extractor.unraveldocs.admin.interfaces.AdminSubscriptionStatsService;
import com.extractor.unraveldocs.subscription.datamodel.BillingIntervalUnit;
import com.extractor.unraveldocs.subscription.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSubscriptionStatsServiceImpl implements AdminSubscriptionStatsService {

    private final UserSubscriptionRepository userSubscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatsDto getSubscriptionStatistics() {
        log.info("Aggregating subscription statistics for admin dashboard");

        long total = userSubscriptionRepository.count();

        // Breakdowns
        Map<String, Long> byPlan = convertToMap(userSubscriptionRepository.countUsersByPlan());
        Map<String, Long> byStatus = convertToMapStatus(userSubscriptionRepository.countUsersByStatus());
        Map<String, Long> bySource = convertToMapStatus(userSubscriptionRepository.countBySubscriptionSourceGrouped());

        // Rates
        long activePaid = userSubscriptionRepository.countActivePaidSubscriptions(); // Need to add to repo or compute
        
        long totalTrials = userSubscriptionRepository.countByHasUsedTrialTrueAndStatusIgnoreCase("ACTIVE") 
                         + userSubscriptionRepository.countByHasUsedTrialTrueAndStatusIgnoreCase("EXPIRED")
                         + userSubscriptionRepository.countByHasUsedTrialTrueAndStatusIgnoreCase("CANCELLED");
        
        long convertedTrials = userSubscriptionRepository.countByHasUsedTrialTrueAndStatusIgnoreCase("ACTIVE");
        double trialConversionRate = totalTrials > 0 ? (double) convertedTrials / totalTrials * 100 : 0.0;

        long churned = userSubscriptionRepository.countByStatusInIgnoreCase(List.of("EXPIRED", "CANCELLED"));
        double churnRate = total > 0 ? (double) churned / total * 100 : 0.0;

        // MRR & ARPU Calculation
        BigDecimal mrr = calculateMrr();
        BigDecimal arpu = activePaid > 0 
                ? mrr.divide(BigDecimal.valueOf(activePaid), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        // Quota Warnings
        SubscriptionStatsDto.QuotaWarningStats warnings = SubscriptionStatsDto.QuotaWarningStats.builder()
                .storage(userSubscriptionRepository.countUsersNearStorageLimit())
                .ocrPages(userSubscriptionRepository.countUsersNearOcrLimit())
                .documentUploads(userSubscriptionRepository.countUsersNearDocLimit())
                .aiOperations(userSubscriptionRepository.countUsersNearAiLimit())
                .build();

        return SubscriptionStatsDto.builder()
                .totalSubscriptions(total)
                .byPlan(byPlan)
                .byStatus(byStatus)
                .bySource(bySource)
                .trialConversionRate(Math.round(trialConversionRate * 100.0) / 100.0)
                .churnRate(Math.round(churnRate * 100.0) / 100.0)
                .mrr(mrr.setScale(2, RoundingMode.HALF_UP))
                .averageRevenuePerUser(arpu)
                .usersNearingQuotaLimits(warnings)
                .build();
    }

    private Map<String, Long> convertToMap(List<Object[]> results) {
        return results.stream().collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> ((Number) row[1]).longValue()
        ));
    }
    
    private Map<String, Long> convertToMapStatus(List<Object[]> results) {
        return results.stream().collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> ((Number) row[1]).longValue()
        ));
    }

    private BigDecimal calculateMrr() {
        List<Object[]> rows = userSubscriptionRepository.sumPricesForActivePaidSubscriptionsGroupedByInterval();
        BigDecimal totalMrr = BigDecimal.ZERO;

        for (Object[] row : rows) {
            Integer intervalValue = (Integer) row[0];
            BillingIntervalUnit unit = (BillingIntervalUnit) row[1];
            BigDecimal sumPrice = (BigDecimal) row[2];

            if (sumPrice == null) continue;

            // Convert everything to Monthly Recurring Revenue
            if (unit == BillingIntervalUnit.MONTH) {
                if (intervalValue == 1) {
                    totalMrr = totalMrr.add(sumPrice);
                } else {
                    totalMrr = totalMrr.add(sumPrice.divide(BigDecimal.valueOf(intervalValue), 2, RoundingMode.HALF_UP));
                }
            } else if (unit == BillingIntervalUnit.YEAR) {
                int months = intervalValue * 12;
                totalMrr = totalMrr.add(sumPrice.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP));
            } else if (unit == BillingIntervalUnit.WEEK) {
                // Approximate 4.33 weeks in a month
                double multiplier = 4.33 / intervalValue;
                totalMrr = totalMrr.add(sumPrice.multiply(BigDecimal.valueOf(multiplier)));
            }
        }
        return totalMrr;
    }
}
