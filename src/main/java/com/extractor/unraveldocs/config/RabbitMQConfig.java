package com.extractor.unraveldocs.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // OCR Exchange and Queue constants
    public static final String OCR_EXCHANGE_NAME = "unraveldocs-ocr-exchange";
    public static final String OCR_QUEUE_NAME = "unraveldocs-ocr-queue";
    public static final String OCR_ROUTING_KEY = "unraveldocs.ocr.#";

    // User Events Exchange and Queue constants
    public static final String USER_EVENTS_EXCHANGE_NAME = "unraveldocs-user-events-exchange";
    public static final String USER_EVENTS_QUEUE_NAME = "unraveldocs-user-events-queue";
    public static final String USER_EVENTS_ROUTING_KEY_PATTERN = "user.#";


    @Bean
    public TopicExchange ocrExchange() {
        return new TopicExchange(OCR_EXCHANGE_NAME);
    }

    @Bean
    public Queue ocrQueue() {
        return new Queue(OCR_QUEUE_NAME, true);
    }

    @Bean
    public Binding ocrBinding(@Qualifier("ocrQueue") Queue ocrQueue, @Qualifier("ocrExchange") TopicExchange ocrExchange) {
        return BindingBuilder.bind(ocrQueue).to(ocrExchange).with(OCR_ROUTING_KEY);
    }

    // User Events Beans
    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE_NAME);
    }

    @Bean
    public Queue userEventsQueue() {
        return new Queue(USER_EVENTS_QUEUE_NAME, true);
    }

    @Bean
    public Binding userEventsBinding(@Qualifier("userEventsQueue") Queue userEventsQueue, @Qualifier("userEventsExchange") TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userEventsQueue).to(userEventsExchange).with(USER_EVENTS_ROUTING_KEY_PATTERN);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
