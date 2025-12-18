package com.extractor.unraveldocs.payment.receipt.repository;

import com.extractor.unraveldocs.payment.receipt.enums.PaymentProvider;
import com.extractor.unraveldocs.payment.receipt.model.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, String> {

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Page<Receipt> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    boolean existsByExternalPaymentIdAndPaymentProvider(String externalPaymentId, PaymentProvider paymentProvider);

    Optional<Receipt> findByExternalPaymentIdAndPaymentProvider(String externalPaymentId, PaymentProvider paymentProvider);
}
