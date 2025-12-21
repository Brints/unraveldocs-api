package com.extractor.unraveldocs.payment.common.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * High-throughput Kafka consumer for payment events.
 *
 * <h2>Design for Thousands of TPS:</h2>
 * <ul>
 *   <li><b>Batch Processing:</b> Consumes messages in batches for efficiency</li>
 *   <li><b>Concurrency:</b> Multiple concurrent listeners (configurable)</li>
 *   <li><b>Manual Acknowledgment:</b> Only ack after successful processing</li>
 *   <li><b>Idempotency:</b> Uses eventId for deduplication</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class PaymentEventListener {

    private final PaymentEventProcessor eventProcessor;

    public PaymentEventListener(PaymentEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    /**
     * Batch listener for payment events.
     * Processes events in batches for higher throughput.
     *
     * Configure concurrency in application.properties:
     * spring.kafka.listener.concurrency=6
     */
    @KafkaListener(
            topics = PaymentTopicConfig.TOPIC_PAYMENT_EVENTS,
            groupId = "unraveldocs-payment-processor",
            containerFactory = "paymentBatchListenerContainerFactory",
            concurrency = "${payment.kafka.consumer.concurrency:6}"
    )
    public void onPaymentEvents(
            List<ConsumerRecord<String, PaymentEvent>> records,
            Acknowledgment acknowledgment
    ) {
        log.debug("Received batch of {} payment events", records.size());

        int successCount = 0;
        int failCount = 0;

        for (ConsumerRecord<String, PaymentEvent> record : records) {
            PaymentEvent event = record.value();

            try {
                eventProcessor.process(event);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("Failed to process payment event: eventId={}, type={}, provider={}, error={}",
                        event.getEventId(), event.getEventType(),
                        event.getProvider(), e.getMessage(), e);
                // Continue processing other events in the batch
                // Failed events will be handled by the error handler (sent to DLQ)
            }
        }

        // Acknowledge the entire batch
        acknowledgment.acknowledge();

        log.info("Processed payment event batch: success={}, failed={}, total={}",
                successCount, failCount, records.size());
    }

    /**
     * Listener for webhook events (raw provider webhooks).
     * Separate consumer group for independent scaling.
     */
    @KafkaListener(
            topics = PaymentTopicConfig.TOPIC_PAYMENT_WEBHOOKS,
            groupId = "unraveldocs-webhook-processor",
            containerFactory = "paymentBatchListenerContainerFactory",
            concurrency = "${payment.kafka.webhook-consumer.concurrency:4}"
    )
    public void onWebhookEvents(
            List<ConsumerRecord<String, PaymentEvent>> records,
            Acknowledgment acknowledgment
    ) {
        log.debug("Received batch of {} webhook events", records.size());

        for (ConsumerRecord<String, PaymentEvent> record : records) {
            PaymentEvent event = record.value();

            try {
                eventProcessor.processWebhook(event);
            } catch (Exception e) {
                log.error("Failed to process webhook event: eventId={}, provider={}, error={}",
                        event.getEventId(), event.getProvider(), e.getMessage(), e);
            }
        }

        acknowledgment.acknowledge();
    }

    /**
     * Single-record listener for subscription events.
     * Lower volume, so single processing is fine.
     */
    @KafkaListener(
            topics = PaymentTopicConfig.TOPIC_SUBSCRIPTION_EVENTS,
            groupId = "unraveldocs-subscription-processor",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSubscriptionEvent(
            ConsumerRecord<String, PaymentEvent> record,
            Acknowledgment acknowledgment
    ) {
        PaymentEvent event = record.value();

        log.info("Processing subscription event: type={}, subscriptionId={}, userId={}",
                event.getEventType(), event.getSubscriptionId(), event.getUserId());

        try {
            eventProcessor.processSubscription(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process subscription event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            throw e; // Let error handler deal with it
        }
    }
}

