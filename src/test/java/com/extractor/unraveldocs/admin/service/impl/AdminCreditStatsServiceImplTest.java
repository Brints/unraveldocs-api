package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.CreditStatsDto;
import com.extractor.unraveldocs.credit.datamodel.CreditTransactionType;
import com.extractor.unraveldocs.credit.repository.CreditPackRepository;
import com.extractor.unraveldocs.credit.repository.CreditTransactionRepository;
import com.extractor.unraveldocs.credit.repository.UserCreditBalanceRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCreditStatsServiceImplTest {

    @Mock
    private UserCreditBalanceRepository userCreditBalanceRepository;

    @Mock
    private CreditTransactionRepository creditTransactionRepository;

    @Mock
    private CreditPackRepository creditPackRepository;

    @Mock
    private ResponseBuilderService responseBuilderService;

    @InjectMocks
    private AdminCreditStatsServiceImpl adminCreditStatsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getCreditStats_Success() {
        // Arrange
        List<Object[]> rawTransactions = new ArrayList<>();
        rawTransactions.add(new Object[]{CreditTransactionType.PURCHASE, 50L});
        rawTransactions.add(new Object[]{CreditTransactionType.DEDUCTION, 120L});

        when(creditTransactionRepository.countTransactionsByType()).thenReturn(rawTransactions);
        when(userCreditBalanceRepository.sumTotalBalance()).thenReturn(5000L);
        when(userCreditBalanceRepository.sumTotalPurchased()).thenReturn(10000L);
        when(userCreditBalanceRepository.sumTotalUsed()).thenReturn(5000L);
        when(creditTransactionRepository.count()).thenReturn(170L);
        when(creditPackRepository.countByIsActiveTrue()).thenReturn(3L);
        when(userCreditBalanceRepository.countUsersWithZeroBalance()).thenReturn(15L);
        when(userCreditBalanceRepository.averageCreditBalance()).thenReturn(new BigDecimal("125.50"));

        UnravelDocsResponse<CreditStatsDto> expectedResponse = new UnravelDocsResponse<>(
                200,
                "success",
                "Credit stats retrieved successfully",
                CreditStatsDto.builder()
                        .totalCreditsInCirculation(5000L)
                        .totalCreditsPurchased(10000L)
                        .totalCreditsUsed(5000L)
                        .totalCreditTransactions(170L)
                        .transactionsByType(Map.of(
                                "PURCHASE", 50L,
                                "DEDUCTION", 120L
                        ))
                        .activeCreditPacks(3L)
                        .usersWithZeroBalance(15L)
                        .averageCreditBalance(new BigDecimal("125.50"))
                        .build()
        );

        when(responseBuilderService.buildUserResponse(
                any(CreditStatsDto.class),
                eq(HttpStatus.OK),
                eq("Credit stats retrieved successfully")
        )).thenReturn(expectedResponse);

        // Act
        UnravelDocsResponse<CreditStatsDto> response = adminCreditStatsService.getCreditStats();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("success", response.getStatus());

        CreditStatsDto data = response.getData();
        assertNotNull(data);
        assertEquals(5000L, data.getTotalCreditsInCirculation());
        assertEquals(10000L, data.getTotalCreditsPurchased());
        assertEquals(5000L, data.getTotalCreditsUsed());
        assertEquals(170L, data.getTotalCreditTransactions());
        assertEquals(3L, data.getActiveCreditPacks());
        assertEquals(15L, data.getUsersWithZeroBalance());
        assertEquals(new BigDecimal("125.50"), data.getAverageCreditBalance());

        Map<String, Long> txnByType = data.getTransactionsByType();
        assertEquals(50L, txnByType.get("PURCHASE"));
        assertEquals(120L, txnByType.get("DEDUCTION"));
    }
}
