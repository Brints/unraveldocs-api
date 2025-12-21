package com.extractor.unraveldocs.payment.common.events;

import com.extractor.unraveldocs.messagequeuing.core.Message;
import com.extractor.unraveldocs.messagequeuing.core.MessageResult;
import com.extractor.unraveldocs.messagequeuing.kafka.producer.KafkaMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * High-throughput payment event publisher for Kafka.
 * Designed to handle thousands of payment transactions per second.
 *
 * <p>Key features for high throughput:</p>
 * <ul>
 *   <li>Async non-blocking publishing</li>
 *   <li>Partitioning by user ID for ordering guarantees per user</li>
 *   <li>Batch-friendly design</li>
 *   <li>Idempotency via event ID</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class PaymentEventPublisher {

    private final KafkaMessageProducer<PaymentEvent> kafkaMessageProducer;

    /**
     * Publish a payment event asynchronously (fire-and-forget with logging).
     * Use this for non-critical events where you don't need confirmation.
     *
     * @param event The payment event to publish
     */
    public void publishAsync(PaymentEvent event) {
        String topic = resolveTopicForEvent();
        String partitionKey = resolvePartitionKey(event);

        log.debug("Publishing payment event async: type={}, provider={}, paymentId={}, eventId={}",
                event.getEventType(), event.getProvider(),
                event.getProviderPaymentId(), event.getEventId());

        Message<PaymentEvent> message = Message.of(event, topic, partitionKey);

        kafkaMessageProducer.send(message)
                .thenAccept(result -> {
                    if (result.success()) {
                        log.debug("Payment event published successfully: eventId={}, partition={}, offset={}",
                                event.getEventId(), result.partition(), result.offset());
                    } else {
                        log.error("Failed to publish payment event: eventId={}, error={}",
                                event.getEventId(), result.errorMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Exception publishing payment event: eventId={}, error={}",
                            event.getEventId(), ex.getMessage(), ex);
                    return null;
                });
    }

    /**
     * Publish a payment event and return a future for the result.
     * Use this when you need to confirm the event was published.
     *
     * @param event The payment event to publish
     * @return CompletableFuture with the publish result
     */
    public CompletableFuture<MessageResult> publish(PaymentEvent event) {
        String topic = resolveTopicForEvent();
        String partitionKey = resolvePartitionKey(event);

        log.debug("Publishing payment event: type={}, provider={}, paymentId={}, eventId={}",
                event.getEventType(), event.getProvider(),
                event.getProviderPaymentId(), event.getEventId());

        Message<PaymentEvent> message = Message.of(event, topic, partitionKey);

        return kafkaMessageProducer.send(message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment event: eventId={}, error={}",
                                event.getEventId(), ex.getMessage());
                    } else if (!result.success()) {
                        log.error("Payment event publish failed: eventId={}, error={}",
                                event.getEventId(), result.errorMessage());
                    } else {
                        log.info("Payment event published: type={}, eventId={}, partition={}, offset={}",
                                event.getEventType(), event.getEventId(),
                                result.partition(), result.offset());
                    }
                });
    }

    /**
     * Publish a payment event synchronously and wait for confirmation.
     * Use sparingly - only when you absolutely need confirmation before proceeding.
     *
     * @param event The payment event to publish
     * @return The publish result
     */
    public MessageResult publishSync(PaymentEvent event) {
        String topic = resolveTopicForEvent();
        String partitionKey = resolvePartitionKey(event);

        log.debug("Publishing payment event sync: type={}, provider={}, paymentId={}",
                event.getEventType(), event.getProvider(), event.getProviderPaymentId());

        Message<PaymentEvent> message = Message.of(event, topic, partitionKey);

        return kafkaMessageProducer.sendAndWait(message);
    }

    /**
     * Resolve the appropriate Kafka topic based on event type.
     * This allows for separate topics for different event categories if needed.
     */
    private String resolveTopicForEvent() {
        return PaymentTopicConfig.TOPIC_PAYMENT_EVENTS;
    }

    /**
     * Resolve the partition key for ordering guarantees.
     * Using userId ensures all events for a user go to the same partition,
     * maintaining order for that user's payment events.
     */
    private String resolvePartitionKey(PaymentEvent event) {
        // Partition by user ID to maintain order per user
        // Falls back to payment reference if user ID is not available
        if (event.getUserId() != null) {
            return event.getUserId();
        }
        if (event.getPaymentReference() != null) {
            return event.getPaymentReference();
        }
        return event.getProviderPaymentId();
    }
}

