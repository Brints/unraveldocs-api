package com.extractor.unraveldocs.payment.paypal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Entity for tracking PayPal webhook events for idempotency.
 */
@Data
@Entity
@Builder
@Table(name = "paypal_webhook_events", indexes = {
        @Index(name = "idx_paypal_webhook_event_id", columnList = "event_id"),
        @Index(name = "idx_paypal_webhook_event_type", columnList = "event_type"),
        @Index(name = "idx_paypal_webhook_processed", columnList = "processed")
})
@NoArgsConstructor
@AllArgsConstructor
public class PayPalWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * PayPal event ID for idempotency.
     */
    @Column(name = "event_id", unique = true, nullable = false)
    private String eventId;

    /**
     * Event type (e.g., PAYMENT.CAPTURE.COMPLETED).
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * Resource type (e.g., capture, subscription).
     */
    @Column(name = "resource_type")
    private String resourceType;

    /**
     * Resource ID from the event.
     */
    @Column(name = "resource_id")
    private String resourceId;

    /**
     * Full event payload in JSON format.
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * Whether the event has been processed.
     */
    @Builder.Default
    private Boolean processed = false;

    /**
     * Time when the event was processed.
     */
    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    /**
     * Error message if processing failed.
     */
    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private OffsetDateTime createdAt;
}
