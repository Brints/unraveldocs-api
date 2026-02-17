package com.extractor.unraveldocs.credit.model;

import com.extractor.unraveldocs.credit.datamodel.CreditTransactionType;
import com.extractor.unraveldocs.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Entity representing a credit transaction (audit log).
 * Records every credit change: purchases, deductions, refunds, bonuses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "credit_transactions")
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditTransactionType type;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_id")
    private String referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
