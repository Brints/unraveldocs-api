package com.extractor.unraveldocs.credit.model;

import com.extractor.unraveldocs.credit.datamodel.CreditPackName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Entity representing a credit pack available for purchase.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "credit_packs")
public class CreditPack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private CreditPackName name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "price_in_cents", nullable = false)
    private Long priceInCents;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "credits_included", nullable = false)
    private Integer creditsIncluded;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "paystack_product_code")
    private String paystackProductCode;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "paypal_product_id")
    private String paypalProductId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
