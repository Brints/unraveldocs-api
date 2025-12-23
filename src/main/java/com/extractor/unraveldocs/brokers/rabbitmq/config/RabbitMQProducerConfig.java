package com.extractor.unraveldocs.brokers.rabbitmq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ producer configuration.
 * Configures RabbitTemplate with JSON serialization and publisher confirms.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMQProducerConfig {

    @Bean
    @Primary
    public MessageConverter rabbitMessageConverter(JsonMapper jsonMapper, DefaultClassMapper classMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setCreateMessageIds(true);
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);

        // Enable publisher confirms for reliability
        template.setMandatory(true);

        // Set up confirm callback to track message acknowledgments
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Message confirmed by broker. CorrelationData: {}",
                        correlationData != null ? correlationData.getId() : "null");
            } else {
                log.error("Message NOT confirmed by broker. CorrelationData: {}, Cause: {}",
                        correlationData != null ? correlationData.getId() : "null", cause);
            }
        });

        // Set up returns callback for unroutable messages
        template.setReturnsCallback(returned -> {
            log.error("Message returned as unroutable. Exchange: {}, RoutingKey: {}, ReplyCode: {}, ReplyText: {}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText());
        });

        return template;
    }
}
