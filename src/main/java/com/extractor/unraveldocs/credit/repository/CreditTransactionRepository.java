package com.extractor.unraveldocs.credit.repository;

import com.extractor.unraveldocs.credit.model.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, String> {

    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CreditTransaction t " +
            "WHERE t.user.id = :userId AND t.type = com.extractor.unraveldocs.credit.datamodel.CreditTransactionType.TRANSFER_SENT "
            +
            "AND t.createdAt >= :startOfMonth")
    int sumTransfersSentInPeriod(@Param("userId") String userId,
            @Param("startOfMonth") OffsetDateTime startOfMonth);
}
