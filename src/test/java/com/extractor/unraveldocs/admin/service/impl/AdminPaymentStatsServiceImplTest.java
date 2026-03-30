package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.PaymentStatsDto;
import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.repository.ReceiptRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPaymentStatsServiceImplTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @InjectMocks
    private AdminPaymentStatsServiceImpl adminPaymentStatsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getPaymentStatistics_success() {
        // Mock simple aggregations
        when(receiptRepository.count()).thenReturn(1500L);
        when(receiptRepository.sumTotalRevenue()).thenReturn(Optional.of(new BigDecimal("25000.00")));
        when(receiptRepository.getAverageTransactionValue()).thenReturn(Optional.of(new BigDecimal("16.67")));
        when(receiptRepository.countByEmailSentFalse()).thenReturn(15L);

        // Mock time-windowed revenues
        when(receiptRepository.sumRevenueAfter(any(OffsetDateTime.class)))
                .thenReturn(Optional.of(new BigDecimal("500.00")))
                .thenReturn(Optional.of(new BigDecimal("3500.00")))
                .thenReturn(Optional.of(new BigDecimal("12000.00")));

        // Mock grouped revenues
        Object[] stripeGroup = new Object[]{PaymentProvider.STRIPE.name(), new BigDecimal("15000.00")};
        Object[] paypalGroup = new Object[]{PaymentProvider.PAYPAL.name(), new BigDecimal("10000.00")};
        when(receiptRepository.sumRevenueByPaymentProviderGrouped()).thenReturn(List.<Object[]>of(stripeGroup, paypalGroup));

        Object[] usdGroup = new Object[]{"USD", new BigDecimal("20000.00")};
        Object[] ngnGroup = new Object[]{"NGN", new BigDecimal("5000.00")};
        when(receiptRepository.sumRevenueByCurrencyGrouped()).thenReturn(List.<Object[]>of(usdGroup, ngnGroup));

        Object[] cardGroup = new Object[]{"CARD", new BigDecimal("25000.00")};
        when(receiptRepository.sumRevenueByPaymentMethodGrouped()).thenReturn(List.<Object[]>of(cardGroup));

        // Mock responseBuilderService
        when(responseBuilderService.buildUserResponse(any(PaymentStatsDto.class), eq(HttpStatus.OK), anyString()))
                .thenAnswer(invocation -> {
                    PaymentStatsDto dto = invocation.getArgument(0);
                    return new UnravelDocsResponse<>(HttpStatus.OK.value(), "success", "Payment stats retrieved successfully", dto);
                });

        // Execute
        UnravelDocsResponse<PaymentStatsDto> response = adminPaymentStatsService.getPaymentStatistics();

        // Verify
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getStatus()).isEqualTo("success");
        
        PaymentStatsDto data = response.getData();
        assertThat(data.getTotalTransactions()).isEqualTo(1500L);
        assertThat(data.getTotalRevenue()).isEqualTo(new BigDecimal("25000.00"));
        assertThat(data.getAverageTransactionValue()).isEqualTo(new BigDecimal("16.67"));
        assertThat(data.getPendingReceiptEmails()).isEqualTo(15L);

        assertThat(data.getRevenueToday()).isEqualTo(new BigDecimal("500.00"));
        assertThat(data.getRevenueThisWeek()).isEqualTo(new BigDecimal("3500.00"));
        assertThat(data.getRevenueThisMonth()).isEqualTo(new BigDecimal("12000.00"));

        assertThat(data.getRevenueByProvider())
                .hasSize(2)
                .containsEntry("STRIPE", new BigDecimal("15000.00"))
                .containsEntry("PAYPAL", new BigDecimal("10000.00"));

        assertThat(data.getRevenueByCurrency())
                .hasSize(2)
                .containsEntry("USD", new BigDecimal("20000.00"));

        assertThat(data.getRevenueByPaymentMethod())
                .hasSize(1)
                .containsEntry("CARD", new BigDecimal("25000.00"));
    }

    @Test
    void getPaymentStatistics_emptyData_returnsZeros() {
        when(receiptRepository.count()).thenReturn(0L);
        when(receiptRepository.sumTotalRevenue()).thenReturn(Optional.empty());
        when(receiptRepository.getAverageTransactionValue()).thenReturn(Optional.empty());
        when(receiptRepository.countByEmailSentFalse()).thenReturn(0L);
        when(receiptRepository.sumRevenueAfter(any(OffsetDateTime.class))).thenReturn(Optional.empty());
        when(receiptRepository.sumRevenueByPaymentProviderGrouped()).thenReturn(Collections.emptyList());
        when(receiptRepository.sumRevenueByCurrencyGrouped()).thenReturn(Collections.emptyList());
        when(receiptRepository.sumRevenueByPaymentMethodGrouped()).thenReturn(Collections.emptyList());

        when(responseBuilderService.buildUserResponse(any(PaymentStatsDto.class), eq(HttpStatus.OK), anyString()))
                .thenAnswer(invocation -> {
                    PaymentStatsDto dto = invocation.getArgument(0);
                    return new UnravelDocsResponse<>(HttpStatus.OK.value(), "success", "Payment stats retrieved successfully", dto);
                });

        UnravelDocsResponse<PaymentStatsDto> response = adminPaymentStatsService.getPaymentStatistics();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        PaymentStatsDto data = response.getData();
        assertThat(data.getTotalTransactions()).isZero();
        assertThat(data.getTotalRevenue()).isEqualTo(BigDecimal.ZERO);
        assertThat(data.getAverageTransactionValue()).isEqualTo(BigDecimal.ZERO);
        assertThat(data.getRevenueToday()).isEqualTo(BigDecimal.ZERO);
        assertThat(data.getRevenueByProvider()).isEmpty();
    }
}
