package com.extractor.unraveldocs.payment.receipt.repository;

import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, String>, JpaSpecificationExecutor<Receipt> {

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Page<Receipt> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    boolean existsByExternalPaymentIdAndPaymentProvider(String externalPaymentId, PaymentProvider paymentProvider);

    Optional<Receipt> findByExternalPaymentIdAndPaymentProvider(String externalPaymentId, PaymentProvider paymentProvider);

    // Admin Dashboard Aggregations

    @Query("SELECT SUM(r.amount) FROM Receipt r")
    Optional<java.math.BigDecimal> sumTotalRevenue();

    @Query("SELECT AVG(r.amount) FROM Receipt r")
    Optional<java.math.BigDecimal> getAverageTransactionValue();

    @Query("SELECT r.paymentProvider, SUM(r.amount) FROM Receipt r GROUP BY r.paymentProvider")
    java.util.List<Object[]> sumRevenueByPaymentProviderGrouped();

    @Query("SELECT r.currency, SUM(r.amount) FROM Receipt r GROUP BY r.currency")
    java.util.List<Object[]> sumRevenueByCurrencyGrouped();

    @Query("SELECT r.paymentMethod, SUM(r.amount) FROM Receipt r GROUP BY r.paymentMethod")
    java.util.List<Object[]> sumRevenueByPaymentMethodGrouped();

    @Query("SELECT SUM(r.amount) FROM Receipt r WHERE r.paidAt >= :after")
    Optional<java.math.BigDecimal> sumRevenueAfter(@org.springframework.data.repository.query.Param("after") java.time.OffsetDateTime after);

    long countByEmailSentFalse();
}
