package com.extractor.unraveldocs.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherService {
    private final RabbitTemplate rabbitTemplate;
    @Getter
    private final MessageConverter messageConverter;

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
        }
    }
}