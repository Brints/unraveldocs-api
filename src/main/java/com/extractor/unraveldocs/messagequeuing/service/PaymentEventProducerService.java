package com.extractor.unraveldocs.messagequeuing.service;

import com.extractor.unraveldocs.messagequeuing.core.Message;
import com.extractor.unraveldocs.messagequeuing.core.MessageBrokerFactory;
import com.extractor.unraveldocs.messagequeuing.core.MessageResult;
import com.extractor.unraveldocs.messagequeuing.kafka.config.KafkaTopicConfig;
import com.extractor.unraveldocs.messagequeuing.messages.PaymentEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Service for producing payment event messages to Kafka.
 * Provides a high-level API for publishing payment events across the system.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducerService {
    
    private final MessageBrokerFactory messageBrokerFactory;
    
    /**
     * Publish a payment success event.
     *
     * @param paymentId Internal payment ID
     * @param providerPaymentId Provider's payment ID
     * @param provider Payment provider name
     * @param userId User ID
     * @param amount Payment amount
     * @param currency Currency code
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> publishPaymentSucceeded(
            String paymentId,
            String providerPaymentId,
            String provider,
            String userId,
            BigDecimal amount,
            String currency
    ) {
        PaymentEventMessage event = PaymentEventMessage.paymentSucceeded(
                paymentId,
                providerPaymentId,
                provider,
                userId,
                amount,
                currency
        );
        
        return sendPaymentEvent(event);
    }
    
    /**
     * Publish a payment failure event.
     *
     * @param paymentId Internal payment ID
     * @param providerPaymentId Provider's payment ID
     * @param provider Payment provider name
     * @param userId User ID
     * @param failureReason Reason for failure
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> publishPaymentFailed(
            String paymentId,
            String providerPaymentId,
            String provider,
            String userId,
            String failureReason
    ) {
        PaymentEventMessage event = PaymentEventMessage.paymentFailed(
                paymentId,
                providerPaymentId,
                provider,
                userId,
                failureReason
        );
        
        return sendPaymentEvent(event);
    }
    
    /**
     * Publish a subscription created event.
     *
     * @param paymentId Internal payment ID
     * @param providerPaymentId Provider's payment ID
     * @param provider Payment provider name
     * @param userId User ID
     * @param subscriptionId Subscription ID
     * @param amount Subscription amount
     * @param currency Currency code
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> publishSubscriptionCreated(
            String paymentId,
            String providerPaymentId,
            String provider,
            String userId,
            String subscriptionId,
            BigDecimal amount,
            String currency
    ) {
        PaymentEventMessage event = PaymentEventMessage.subscriptionCreated(
                paymentId,
                providerPaymentId,
                provider,
                userId,
                subscriptionId,
                amount,
                currency
        );
        
        return sendPaymentEvent(event);
    }
    
    /**
     * Publish a refund event.
     *
     * @param paymentId Original payment ID
     * @param providerPaymentId Provider's payment ID
     * @param provider Payment provider name
     * @param userId User ID
     * @param refundAmount Refund amount
     * @param currency Currency code
     * @return CompletableFuture with the send result
     */
    public CompletableFuture<MessageResult> publishRefund(
            String paymentId,
            String providerPaymentId,
            String provider,
            String userId,
            BigDecimal refundAmount,
            String currency
    ) {
        PaymentEventMessage event = PaymentEventMessage.refunded(
                paymentId,
                providerPaymentId,
                provider,
                userId,
                refundAmount,
                currency
        );
        
        return sendPaymentEvent(event);
    }
    
    /**
     * Send a payment event to Kafka.
     */
    private CompletableFuture<MessageResult> sendPaymentEvent(PaymentEventMessage event) {
        log.debug("Publishing payment event. Type: {}, PaymentId: {}",
                event.eventType(), event.paymentId());
        
        Message<PaymentEventMessage> message = Message.of(
                event,
                KafkaTopicConfig.TOPIC_PAYMENTS,
                event.userId() // Use user ID as partition key
        );
        
        return messageBrokerFactory.<PaymentEventMessage>getDefaultProducer()
                .send(message)
                .thenApply(result -> {
                    if (result.success()) {
                        log.info("Payment event published. Type: {}, PaymentId: {}, MessageId: {}",
                                event.eventType(), event.paymentId(), result.messageId());
                    } else {
                        log.warn("Failed to publish payment event. Type: {}, PaymentId: {}, Error: {}",
                                event.eventType(), event.paymentId(), result.errorMessage());
                    }
                    return result;
                });
    }
}
