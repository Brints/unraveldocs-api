package com.extractor.unraveldocs.payment.paypal.repository;

import com.extractor.unraveldocs.payment.paypal.model.PayPalWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PayPal webhook event operations (idempotency).
 */
@Repository
public interface PayPalWebhookEventRepository extends JpaRepository<PayPalWebhookEvent, String> {

    Optional<PayPalWebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
