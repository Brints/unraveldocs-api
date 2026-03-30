package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.PaymentStatsDto;
import com.extractor.unraveldocs.admin.interfaces.AdminPaymentStatsService;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.repository.ReceiptRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentStatsServiceImpl implements AdminPaymentStatsService {

    private final ReceiptRepository receiptRepository;
    private final ResponseBuilderService responseBuilder;

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<PaymentStatsDto> getPaymentStatistics() {
        log.info("Fetching payment statistics for admin dashboard");

        long totalTransactions = receiptRepository.count();
        BigDecimal totalRevenue = receiptRepository.sumTotalRevenue().orElse(BigDecimal.ZERO);
        BigDecimal averageTransactionValue = receiptRepository.getAverageTransactionValue().orElse(BigDecimal.ZERO);
        long pendingReceiptEmails = receiptRepository.countByEmailSentFalse();

        // Calculate time-windowed revenues
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime startOfWeek = now.minusDays(7);
        OffsetDateTime startOfMonth = now.minusDays(30);

        BigDecimal revenueToday = receiptRepository.sumRevenueAfter(startOfToday).orElse(BigDecimal.ZERO);
        BigDecimal revenueThisWeek = receiptRepository.sumRevenueAfter(startOfWeek).orElse(BigDecimal.ZERO);
        BigDecimal revenueThisMonth = receiptRepository.sumRevenueAfter(startOfMonth).orElse(BigDecimal.ZERO);

        // Map breakdowns
        Map<String, BigDecimal> revenueByProvider = mapGroupedResults(receiptRepository.sumRevenueByPaymentProviderGrouped());
        Map<String, BigDecimal> revenueByCurrency = mapGroupedResults(receiptRepository.sumRevenueByCurrencyGrouped());
        Map<String, BigDecimal> revenueByPaymentMethod = mapGroupedResults(receiptRepository.sumRevenueByPaymentMethodGrouped());

        PaymentStatsDto stats = PaymentStatsDto.builder()
                .totalTransactions(totalTransactions)
                .totalRevenue(totalRevenue)
                .revenueByProvider(revenueByProvider)
                .revenueByCurrency(revenueByCurrency)
                .revenueByPaymentMethod(revenueByPaymentMethod)
                .averageTransactionValue(averageTransactionValue)
                .pendingReceiptEmails(pendingReceiptEmails)
                .revenueToday(revenueToday)
                .revenueThisWeek(revenueThisWeek)
                .revenueThisMonth(revenueThisMonth)
                .build();

        return responseBuilder.buildUserResponse(stats, HttpStatus.OK, "Payment stats retrieved successfully");
    }

    /**
     * Helper method to map List<Object[]> where Object[0] is the key and Object[1] is the (BigDecimal) value
     */
    private Map<String, BigDecimal> mapGroupedResults(List<Object[]> results) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] result : results) {
            String key = result[0] != null ? result[0].toString() : "UNKNOWN";
            BigDecimal value = result[1] != null ? new BigDecimal(result[1].toString()) : BigDecimal.ZERO;
            map.put(key, value);
        }
        return map;
    }
}
