package com.extractor.unraveldocs.coupon.kafka;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for bulk coupon generation events.
 * Only loaded when Kafka is enabled via coupon.kafka.enabled=true.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class CouponGenerationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SanitizeLogging sanitizer;

    @Value("${coupon.kafka.topic:coupon-generation-events}")
    private String topic;

    /**
     * Publishes a bulk generation command to Kafka.
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<SendResult<String, BulkGenerationCommand>> publishBulkGenerationCommand(
            BulkGenerationCommand command) {

        log.info("Publishing bulk generation command. JobId: {}, UserId: {}, Quantity: {}",
                sanitizer.sanitizeLogging(command.getJobId()),
                sanitizer.sanitizeLogging(command.getUserId()),
                sanitizer.sanitizeLoggingInteger(command.getRequest().getQuantity()));

        return (CompletableFuture<SendResult<String, BulkGenerationCommand>>) (CompletableFuture<?>)
                kafkaTemplate.send(topic, command.getUserId(), command)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish bulk generation command for job: {}",
                                sanitizer.sanitizeLogging(command.getJobId()), ex);
                    } else {
                        log.info("Published bulk generation command for job: {} to partition: {}",
                                sanitizer.sanitizeLogging(command.getJobId()),
                                sanitizer.sanitizeLoggingInteger(result.getRecordMetadata().partition()));
                    }
                });
    }
}
