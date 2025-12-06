package com.extractor.unraveldocs.config;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.auth.events.WelcomeEvent;
import com.extractor.unraveldocs.events.EventTypes;
import com.extractor.unraveldocs.ocrprocessing.events.OcrRequestedEvent;
import com.extractor.unraveldocs.user.events.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    // Admin Events
    public static final String ADMIN_EVENTS_EXCHANGE = "admin.events.exchange";
    public static final String ADMIN_EVENTS_QUEUE = "admin.events.queue";
    public static final String ADMIN_EVENTS_ROUTING_KEY_PATTERN = "admin.#";
    public static final String ADMIN_CREATED_ROUTING_KEY = "admin.created";
    public static final String ADMIN_EVENTS_DLX = ADMIN_EVENTS_EXCHANGE + ".dlx";
    public static final String ADMIN_EVENTS_DLQ = ADMIN_EVENTS_QUEUE + ".dlq";


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

    // Admin Exchange/Queue Beans
    @Bean
    TopicExchange adminEventsExchange() {
        return new TopicExchange(ADMIN_EVENTS_EXCHANGE);
    }

    @Bean
    DirectExchange adminDeadLetterExchange() {
        return new DirectExchange(ADMIN_EVENTS_DLX);
    }

    @Bean
    Queue adminDeadLetterQueue() {
        return new Queue(ADMIN_EVENTS_DLQ);
    }

    @Bean
    Binding adminDeadLetterBinding() {
        return BindingBuilder.bind(adminDeadLetterQueue()).to(adminDeadLetterExchange()).with(ADMIN_EVENTS_QUEUE);
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
        factory.setMessageConverter(messageConverter());
        return factory;
    }

    @Bean
    public MethodInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer((args, cause) -> {
                    // This will cause the message to be rejected and sent to the DLQ
                    throw new AmqpRejectAndDontRequeueException("Failed after retries", cause);
                })
                .build();
    }

    @Bean
    public MessageConverter messageConverter() {
        JacksonJsonMessageConverter jsonConverter = new JacksonJsonMessageConverter();
        jsonConverter.setClassMapper(classMapper());
        return jsonConverter;
    }

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();

        // Map event types to their specific payload classes, not BaseEvent
        idClassMapping.put(EventTypes.USER_REGISTERED, UserRegisteredEvent.class);
        idClassMapping.put(EventTypes.USER_DELETION_SCHEDULED, UserDeletionScheduledEvent.class);
        idClassMapping.put(EventTypes.USER_DELETED, UserDeletedEvent.class);
        idClassMapping.put(EventTypes.PASSWORD_CHANGED, PasswordChangedEvent.class);
        idClassMapping.put(EventTypes.PASSWORD_RESET_REQUESTED, PasswordResetEvent.class);
        idClassMapping.put(EventTypes.PASSWORD_RESET_SUCCESSFUL, PasswordResetSuccessfulEvent.class);
        idClassMapping.put(EventTypes.WELCOME_EVENT, WelcomeEvent.class);
        idClassMapping.put(EventTypes.OCR_REQUESTED, OcrRequestedEvent.class);

        classMapper.setIdClassMapping(idClassMapping);
        classMapper.setTrustedPackages("*");
        return classMapper;
    }
}
