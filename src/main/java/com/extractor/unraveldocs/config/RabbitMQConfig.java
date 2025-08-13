package com.extractor.unraveldocs.config;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.auth.events.WelcomeEvent;
import com.extractor.unraveldocs.user.events.*;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // User Events
    public static final String USER_EVENTS_EXCHANGE = "user.events.exchange";
    public static final String USER_EVENTS_QUEUE = "user.events.queue";
    public static final String USER_EVENTS_ROUTING_KEY_PATTERN = "user.#";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_EVENTS_DLX = USER_EVENTS_EXCHANGE + ".dlx";
    public static final String USER_EVENTS_DLQ = USER_EVENTS_QUEUE + ".dlq";


    // OCR Events
    public static final String OCR_EVENTS_EXCHANGE = "ocr.events.exchange";
    public static final String OCR_EVENTS_QUEUE = "ocr.events.queue";
    public static final String OCR_ROUTING_KEY = "unraveldocs.ocr.request";
    public static final String OCR_EVENTS_DLX = OCR_EVENTS_EXCHANGE + ".dlx";
    public static final String OCR_EVENTS_DLQ = OCR_EVENTS_QUEUE + ".dlq";


    // User Exchange/Queue Beans
    @Bean
    TopicExchange userEventsExchange() {
        return new TopicExchange(USER_EVENTS_EXCHANGE);
    }

    @Bean
    DirectExchange userDeadLetterExchange() {
        return new DirectExchange(USER_EVENTS_DLX);
    }

    @Bean
    Queue userDeadLetterQueue() {
        return new Queue(USER_EVENTS_DLQ);
    }

    @Bean
    Binding userDeadLetterBinding() {
        return BindingBuilder.bind(userDeadLetterQueue()).to(userDeadLetterExchange()).with(USER_EVENTS_QUEUE);
    }

    @Bean
    Queue userEventsQueue() {
        return QueueBuilder.durable(USER_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", USER_EVENTS_QUEUE)
                .build();
    }

    @Bean
    Binding userEventsBinding() {
        return BindingBuilder.bind(userEventsQueue())
                .to(userEventsExchange())
                .with(USER_EVENTS_ROUTING_KEY_PATTERN);
    }

    // OCR Exchange/Queue Beans
    @Bean
    TopicExchange ocrEventsExchange() {
        return new TopicExchange(OCR_EVENTS_EXCHANGE);
    }

    @Bean
    DirectExchange ocrDeadLetterExchange() {
        return new DirectExchange(OCR_EVENTS_DLX);
    }

    @Bean
    Queue ocrDeadLetterQueue() {
        return new Queue(OCR_EVENTS_DLQ);
    }

    @Bean
    Binding ocrDeadLetterBinding() {
        return BindingBuilder.bind(ocrDeadLetterQueue()).to(ocrDeadLetterExchange()).with(OCR_EVENTS_QUEUE);
    }

    @Bean
    Queue ocrEventsQueue() {
        return QueueBuilder.durable(OCR_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", OCR_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", OCR_EVENTS_QUEUE)
                .build();
    }

    @Bean
    Binding ocrEventsBinding() {
        return BindingBuilder.bind(ocrEventsQueue())
                .to(ocrEventsExchange())
                .with(OCR_ROUTING_KEY);
    }


    // Generic Beans (Listener Factory, Retry, Message Converter)
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(retryInterceptor());
        factory.setMessageConverter(messageConverter()); // Ensure the converter is used by the factory
        return factory;
    }

    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer((args, cause) -> {
                    // This will cause the message to be rejected and sent to the DLQ
                    throw new AmqpRejectAndDontRequeueException("Failed after retries", cause);
                })
                .build();
    }

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter jsonConverter = new Jackson2JsonMessageConverter();
        jsonConverter.setClassMapper(classMapper());
        return jsonConverter;
    }

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();

        // Map event types to their specific payload classes, not BaseEvent
        idClassMapping.put("UserRegistered", UserRegisteredEvent.class);
        idClassMapping.put("UserRegisteredEvent", UserRegisteredEvent.class);
        idClassMapping.put("UserDeletionScheduled", UserDeletionScheduledEvent.class);
        idClassMapping.put("UserDeleted", UserDeletedEvent.class);
        idClassMapping.put("PasswordChanged", PasswordChangedEvent.class);
        idClassMapping.put("PasswordResetRequested", PasswordResetEvent.class);
        idClassMapping.put("PasswordResetSuccessful", PasswordResetSuccessfulEvent.class);
        idClassMapping.put("WelcomeEvent", WelcomeEvent.class);

        classMapper.setIdClassMapping(idClassMapping);
        classMapper.setTrustedPackages("*");
        return classMapper;
    }
}
