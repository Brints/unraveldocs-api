package com.extractor.unraveldocs.admin.service.impl;

import com.extractor.unraveldocs.admin.dto.response.CreditStatsDto;
import com.extractor.unraveldocs.admin.interfaces.AdminCreditStatsService;
import com.extractor.unraveldocs.credit.datamodel.CreditTransactionType;
import com.extractor.unraveldocs.credit.repository.CreditPackRepository;
import com.extractor.unraveldocs.credit.repository.CreditTransactionRepository;
import com.extractor.unraveldocs.credit.repository.UserCreditBalanceRepository;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCreditStatsServiceImpl implements AdminCreditStatsService {

    private final UserCreditBalanceRepository userCreditBalanceRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditPackRepository creditPackRepository;
    private final ResponseBuilderService responseBuilderService;

    @Override
    @Transactional(readOnly = true)
    public UnravelDocsResponse<CreditStatsDto> getCreditStats() {
        log.info("Fetching admin credit statistics");

        List<Object[]> transactionsByTypeRaw = creditTransactionRepository.countTransactionsByType();
        Map<String, Long> transactionsByType = new LinkedHashMap<>();
        for (Object[] row : transactionsByTypeRaw) {
            String type = ((CreditTransactionType) row[0]).name();
            Long count = ((Number) row[1]).longValue();
            transactionsByType.put(type, count);
        }

        CreditStatsDto stats = CreditStatsDto.builder()
                .totalCreditsInCirculation(userCreditBalanceRepository.sumTotalBalance())
                .totalCreditsPurchased(userCreditBalanceRepository.sumTotalPurchased())
                .totalCreditsUsed(userCreditBalanceRepository.sumTotalUsed())
                .totalCreditTransactions(creditTransactionRepository.count())
                .transactionsByType(transactionsByType)
                .activeCreditPacks(creditPackRepository.countByIsActiveTrue())
                .usersWithZeroBalance(userCreditBalanceRepository.countUsersWithZeroBalance())
                .averageCreditBalance(userCreditBalanceRepository.averageCreditBalance())
                .build();

        return responseBuilderService.buildUserResponse(
                stats,
                HttpStatus.OK,
                "Credit stats retrieved successfully"
        );
    }
}
