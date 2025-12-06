package com.extractor.unraveldocs.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailRabbitMQConfig {
    public static final String EXCHANGE_NAME = "unraveldocs-email-exchange";
    public static final String QUEUE_NAME = "unraveldocs-email-queue";
    public static final String ROUTING_KEY = "unraveldocs.email.send";

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailQueue).to(emailExchange).with(ROUTING_KEY);
    }

}
