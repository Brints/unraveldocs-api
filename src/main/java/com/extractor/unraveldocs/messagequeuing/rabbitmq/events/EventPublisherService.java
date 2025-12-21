package com.extractor.unraveldocs.messagequeuing.rabbitmq.events;

import com.extractor.unraveldocs.messagequeuing.core.MessageBrokerType;
import com.extractor.unraveldocs.messagequeuing.core.MessagingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class EventPublisherService {
    private final RabbitTemplate rabbitTemplate;
    @Getter
    private final MessageConverter messageConverter;

    /**
     * Publishes an event to RabbitMQ.
     *
     * @param exchange   The exchange to publish to
     * @param routingKey The routing key
     * @param event      The event to publish
     * @param <T>        The payload type
     * @throws MessagingException if publishing fails
     */
    public <T> void publishEvent(String exchange, String routingKey, BaseEvent<T> event) {
        try {
            Object payload = event.getPayload();

            rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
                MessageProperties props = message.getMessageProperties();
                props.setHeader("__TypeId__", event.getMetadata().getEventType());
                props.setCorrelationId(event.getMetadata().getCorrelationId());
                return message;
            });
            log.info("Published event of type '{}' with correlationId '{}' to exchange '{}' with routing key '{}'",
                    event.getMetadata().getEventType(),
                    event.getMetadata().getCorrelationId(),
                    exchange,
                    routingKey);
        } catch (Exception e) {
            log.error("Failed to publish event. CorrelationId: '{}', Exchange: '{}', RoutingKey: '{}'. Error: {}",
                    event.getMetadata().getCorrelationId(), exchange, routingKey, e.getMessage(), e);
            throw MessagingException.sendFailed(
                    MessageBrokerType.RABBITMQ,
                    event.getMetadata().getCorrelationId(),
                    routingKey,
                    e
            );
        }
    }
}