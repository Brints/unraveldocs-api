package com.extractor.unraveldocs.payment.stripe.repository;

import com.extractor.unraveldocs.payment.stripe.model.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for StripeWebhookEvent entity
 */
@Repository
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {
    
    /**
     * Find a webhook event by Stripe event ID
     */
    Optional<StripeWebhookEvent> findByEventId(String eventId);
    
    /**
     * Check if a webhook event exists by event ID
     */
    boolean existsByEventId(String eventId);
    
    /**
     * Find all unprocessed webhook events
     */
    List<StripeWebhookEvent> findByProcessedFalse();
    
    /**
     * Find all webhook events by type
     */
    List<StripeWebhookEvent> findByEventType(String eventType);
    
    /**
     * Find all processed events with errors
     */
    List<StripeWebhookEvent> findByProcessedTrueAndProcessingErrorIsNotNull();
    
    /**
     * Delete old processed events (for cleanup)
     */
    void deleteByProcessedTrueAndCreatedAtBefore(OffsetDateTime date);

    /**
     * Find events ready for retry (not processed, not max retries, next retry time passed)
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT e FROM StripeWebhookEvent e WHERE e.processed = false " +
            "AND e.maxRetriesReached = false " +
            "AND e.processingError IS NOT NULL " +
            "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)")
    List<StripeWebhookEvent> findEventsToRetry(@org.springframework.data.repository.query.Param("now") OffsetDateTime now);

    /**
     * Find all dead letter events (max retries reached)
     */
    List<StripeWebhookEvent> findByMaxRetriesReachedTrue();

    /**
     * Count dead letter events
     */
    long countByMaxRetriesReachedTrue();
}
