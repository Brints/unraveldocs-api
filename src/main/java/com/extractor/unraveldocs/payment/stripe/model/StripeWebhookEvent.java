package com.extractor.unraveldocs.payment.stripe.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Entity for tracking webhook events to ensure idempotent processing
 */
@Data
@Entity
@Table(name = "stripe_webhook_events", indexes = {
        @Index(name = "idx_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_processed", columnList = "processed"),
        @Index(name = "idx_created_at_webhook", columnList = "created_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class StripeWebhookEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean processed = false;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;
}
