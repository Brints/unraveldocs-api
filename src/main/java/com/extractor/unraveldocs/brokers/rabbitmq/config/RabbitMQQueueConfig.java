package com.extractor.unraveldocs.brokers.rabbitmq.config;

import com.extractor.unraveldocs.auth.events.UserRegisteredEvent;
import com.extractor.unraveldocs.auth.events.WelcomeEvent;
import com.extractor.unraveldocs.brokers.rabbitmq.events.EventTypes;
import com.extractor.unraveldocs.elasticsearch.events.ElasticsearchIndexEvent;
import com.extractor.unraveldocs.ocrprocessing.events.OcrRequestedEvent;
import com.extractor.unraveldocs.team.events.TeamTrialExpiringEvent;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ queue and exchange configuration.
 * Defines all queues, exchanges, bindings, and DLQ setup.
 */
@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMQQueueConfig {

    // ==================== Email Events ====================
    public static final String EMAIL_EXCHANGE = "unraveldocs-email-exchange";
    public static final String EMAIL_QUEUE = "unraveldocs-email-queue";
    public static final String EMAIL_ROUTING_KEY = "unraveldocs.email.send";
    public static final String EMAIL_DLX = EMAIL_EXCHANGE + ".dlx";
    public static final String EMAIL_DLQ = EMAIL_QUEUE + ".dlq";

    // ==================== User Events ====================
    public static final String USER_EVENTS_EXCHANGE = "user.events.exchange";
    public static final String USER_EVENTS_QUEUE = "user.events.queue";
    public static final String USER_EVENTS_ROUTING_KEY_PATTERN = "user.#";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";
    public static final String USER_EVENTS_DLX = USER_EVENTS_EXCHANGE + ".dlx";
    public static final String USER_EVENTS_DLQ = USER_EVENTS_QUEUE + ".dlq";

    // ==================== Admin Events ====================
    public static final String ADMIN_EVENTS_EXCHANGE = "admin.events.exchange";
    public static final String ADMIN_EVENTS_QUEUE = "admin.events.queue";
    public static final String ADMIN_EVENTS_ROUTING_KEY_PATTERN = "admin.#";
    public static final String ADMIN_CREATED_ROUTING_KEY = "admin.created";
    public static final String ADMIN_EVENTS_DLX = ADMIN_EVENTS_EXCHANGE + ".dlx";
    public static final String ADMIN_EVENTS_DLQ = ADMIN_EVENTS_QUEUE + ".dlq";

    // ==================== OCR Events ====================
    public static final String OCR_EVENTS_EXCHANGE = "ocr.events.exchange";
    public static final String OCR_EVENTS_QUEUE = "ocr.events.queue";
    public static final String OCR_ROUTING_KEY = "unraveldocs.ocr.request";
    public static final String OCR_EVENTS_DLX = OCR_EVENTS_EXCHANGE + ".dlx";
    public static final String OCR_EVENTS_DLQ = OCR_EVENTS_QUEUE + ".dlq";

    // ==================== Elasticsearch Events ====================
    public static final String ES_EVENTS_EXCHANGE = "elasticsearch.events.exchange";
    public static final String ES_INDEX_QUEUE = "elasticsearch.index.queue";
    public static final String ES_INDEX_ROUTING_KEY = "elasticsearch.index.#";
    public static final String ES_EVENTS_DLX = ES_EVENTS_EXCHANGE + ".dlx";
    public static final String ES_EVENTS_DLQ = ES_INDEX_QUEUE + ".dlq";

    // ==================== Team Events ====================
    public static final String TEAM_EVENTS_EXCHANGE = "team.events.exchange";
    public static final String TEAM_EVENTS_QUEUE = "team.events.queue";
    public static final String TEAM_EVENTS_ROUTING_KEY_PATTERN = "team.#";
    public static final String TEAM_TRIAL_EXPIRING_ROUTING_KEY = "team.trial.expiring";
    public static final String TEAM_SUBSCRIPTION_ROUTING_KEY = "team.subscription.#";
    public static final String TEAM_EVENTS_DLX = TEAM_EVENTS_EXCHANGE + ".dlx";
    public static final String TEAM_EVENTS_DLQ = TEAM_EVENTS_QUEUE + ".dlq";

    // ==================== Email Exchange/Queue Beans ====================
    @Bean
    TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE);
    }

    @Bean
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_QUEUE)
                .build();
    }

    @Bean
    DirectExchange emailDeadLetterExchange() {
        return new DirectExchange(EMAIL_DLX);
    }

    @Bean
    Queue emailDeadLetterQueue() {
        return new Queue(EMAIL_DLQ);
    }

    @Bean
    Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange()).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    Binding emailDeadLetterBinding() {
        return BindingBuilder.bind(emailDeadLetterQueue()).to(emailDeadLetterExchange()).with(EMAIL_QUEUE);
    }

    // ==================== User Exchange/Queue Beans ====================
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

    // ==================== Admin Exchange/Queue Beans ====================
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

    @Bean
    Queue adminEventsQueue() {
        return QueueBuilder.durable(ADMIN_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", ADMIN_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", ADMIN_EVENTS_QUEUE)
                .build();
    }

    @Bean
    Binding adminEventsBinding() {
        return BindingBuilder.bind(adminEventsQueue())
                .to(adminEventsExchange())
                .with(ADMIN_EVENTS_ROUTING_KEY_PATTERN);
    }

    // ==================== OCR Exchange/Queue Beans ====================
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

    // ==================== Elasticsearch Exchange/Queue Beans ====================
    @Bean
    TopicExchange esEventsExchange() {
        return new TopicExchange(ES_EVENTS_EXCHANGE);
    }

    @Bean
    DirectExchange esDeadLetterExchange() {
        return new DirectExchange(ES_EVENTS_DLX);
    }

    @Bean
    Queue esDeadLetterQueue() {
        return new Queue(ES_EVENTS_DLQ);
    }

    @Bean
    Binding esDeadLetterBinding() {
        return BindingBuilder.bind(esDeadLetterQueue()).to(esDeadLetterExchange()).with(ES_INDEX_QUEUE);
    }

    @Bean
    Queue esIndexQueue() {
        return QueueBuilder.durable(ES_INDEX_QUEUE)
                .withArgument("x-dead-letter-exchange", ES_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", ES_INDEX_QUEUE)
                .build();
    }

    @Bean
    Binding esEventsBinding() {
        return BindingBuilder.bind(esIndexQueue())
                .to(esEventsExchange())
                .with(ES_INDEX_ROUTING_KEY);
    }

    // ==================== Team Exchange/Queue Beans ====================
    @Bean
    TopicExchange teamEventsExchange() {
        return new TopicExchange(TEAM_EVENTS_EXCHANGE);
    }

    @Bean
    DirectExchange teamDeadLetterExchange() {
        return new DirectExchange(TEAM_EVENTS_DLX);
    }

    @Bean
    Queue teamDeadLetterQueue() {
        return new Queue(TEAM_EVENTS_DLQ);
    }

    @Bean
    Binding teamDeadLetterBinding() {
        return BindingBuilder.bind(teamDeadLetterQueue()).to(teamDeadLetterExchange()).with(TEAM_EVENTS_QUEUE);
    }

    @Bean
    Queue teamEventsQueue() {
        return QueueBuilder.durable(TEAM_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", TEAM_EVENTS_DLX)
                .withArgument("x-dead-letter-routing-key", TEAM_EVENTS_QUEUE)
                .build();
    }

    @Bean
    Binding teamEventsBinding() {
        return BindingBuilder.bind(teamEventsQueue())
                .to(teamEventsExchange())
                .with(TEAM_EVENTS_ROUTING_KEY_PATTERN);
    }

    // ==================== Listener Configuration ====================
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter listenerMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(retryInterceptor());
        factory.setMessageConverter(listenerMessageConverter);
        return factory;
    }

    @Bean
    public MethodInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer((args, cause) -> {
                    throw new AmqpRejectAndDontRequeueException("Failed after retries", cause);
                })
                .build();
    }

    @Bean
    public MessageConverter listenerMessageConverter() {
        JacksonJsonMessageConverter jsonConverter = new JacksonJsonMessageConverter();
        jsonConverter.setClassMapper(classMapper());
        return jsonConverter;
    }

    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();

        // Map event types to their specific payload classes
        idClassMapping.put(EventTypes.USER_REGISTERED, UserRegisteredEvent.class);
        idClassMapping.put(EventTypes.USER_DELETION_SCHEDULED, UserDeletionScheduledEvent.class);
        idClassMapping.put(EventTypes.USER_DELETED, UserDeletedEvent.class);
        idClassMapping.put(EventTypes.PASSWORD_CHANGED, PasswordChangedEvent.class);
        idClassMapping.put(EventTypes.PASSWORD_RESET_REQUESTED, PasswordResetEvent.class);
        idClassMapping.put(EventTypes.PASSWORD_RESET_SUCCESSFUL, PasswordResetSuccessfulEvent.class);
        idClassMapping.put(EventTypes.WELCOME_EVENT, WelcomeEvent.class);
        idClassMapping.put(EventTypes.OCR_REQUESTED, OcrRequestedEvent.class);

        // Elasticsearch event types
        idClassMapping.put(EventTypes.ES_DOCUMENT_INDEX, ElasticsearchIndexEvent.class);
        idClassMapping.put(EventTypes.ES_USER_INDEX, ElasticsearchIndexEvent.class);
        idClassMapping.put(EventTypes.ES_PAYMENT_INDEX, ElasticsearchIndexEvent.class);
        idClassMapping.put(EventTypes.ES_SUBSCRIPTION_INDEX, ElasticsearchIndexEvent.class);

        // Team event types
        idClassMapping.put(EventTypes.TEAM_TRIAL_EXPIRING, TeamTrialExpiringEvent.class);

        classMapper.setIdClassMapping(idClassMapping);
        classMapper.setTrustedPackages(
                "com.extractor.unraveldocs.auth.events",
                "com.extractor.unraveldocs.user.events",
                "com.extractor.unraveldocs.ocrprocessing.events",
                "com.extractor.unraveldocs.elasticsearch.events",
                "com.extractor.unraveldocs.team.events",
                "com.extractor.unraveldocs.messaging.dto");
        return classMapper;
    }
}
