package com.extractor.unraveldocs.elasticsearch.consumer;

import com.extractor.unraveldocs.brokers.rabbitmq.config.RabbitMQQueueConfig;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.elasticsearch.document.DocumentSearchIndex;
import com.extractor.unraveldocs.elasticsearch.document.PaymentSearchIndex;
import com.extractor.unraveldocs.elasticsearch.document.UserSearchIndex;
import com.extractor.unraveldocs.elasticsearch.events.ElasticsearchIndexEvent;
import com.extractor.unraveldocs.elasticsearch.events.IndexAction;
import com.extractor.unraveldocs.elasticsearch.repository.DocumentSearchRepository;
import com.extractor.unraveldocs.elasticsearch.repository.PaymentSearchRepository;
import com.extractor.unraveldocs.elasticsearch.repository.UserSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ consumer for processing Elasticsearch indexing events.
 * Listens to the Elasticsearch index queue and performs indexing operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class ElasticsearchIndexConsumer {

    private final DocumentSearchRepository documentSearchRepository;
    private final UserSearchRepository userSearchRepository;
    private final PaymentSearchRepository paymentSearchRepository;
    private final ObjectMapper jsonMapper;
    private final SanitizeLogging sanitize;

    /**
     * Processes incoming Elasticsearch indexing events.
     *
     * @param event The indexing event to process
     */
    @RabbitListener(queues = RabbitMQQueueConfig.ES_INDEX_QUEUE)
    public void handleIndexEvent(ElasticsearchIndexEvent event) {
        log.info("Received Elasticsearch index event: type={}, action={}, documentId={}",
                sanitize.sanitizeLogging(String.valueOf(event.getIndexType())),
                sanitize.sanitizeLogging(String.valueOf(event.getAction())),
                sanitize.sanitizeLogging(event.getDocumentId()));

        try {
            switch (event.getIndexType()) {
                case DOCUMENT -> processDocumentEvent(event);
                case USER -> processUserEvent(event);
                case PAYMENT -> processPaymentEvent(event);
                case SUBSCRIPTION -> log.info("Subscription indexing not yet implemented");
            }
            log.info("Successfully processed Elasticsearch index event for document ID: {}",
                    sanitize.sanitizeLogging(event.getDocumentId()));
        } catch (Exception e) {
            log.error("Failed to process Elasticsearch index event: {}", e.getMessage(), e);
            throw e; // Let RabbitMQ retry mechanism handle it
        }
    }

    private void processDocumentEvent(ElasticsearchIndexEvent event) {
        if (event.getAction() == IndexAction.DELETE) {
            documentSearchRepository.deleteById(event.getDocumentId());
            log.debug("Deleted document from index: {}", sanitize.sanitizeLogging(event.getDocumentId()));
        } else {
            if (event.getPayload() == null) {
                throw new IllegalArgumentException("Payload is required for CREATE/UPDATE actions");
            }
            DocumentSearchIndex document = deserialize(event.getPayload(), DocumentSearchIndex.class);
            documentSearchRepository.save(document);
            log.debug("Indexed document: {}", sanitize.sanitizeLogging(event.getDocumentId()));
        }
    }

    private void processUserEvent(ElasticsearchIndexEvent event) {
        if (event.getAction() == IndexAction.DELETE) {
            userSearchRepository.deleteById(event.getDocumentId());
            log.debug("Deleted user from index: {}", sanitize.sanitizeLogging(event.getDocumentId()));
        } else {
            UserSearchIndex user = deserialize(event.getPayload(), UserSearchIndex.class);
            userSearchRepository.save(user);
            log.debug("Indexed user: {}", sanitize.sanitizeLogging(event.getDocumentId()));
        }
    }

    private void processPaymentEvent(ElasticsearchIndexEvent event) {
        if (event.getAction() == IndexAction.DELETE) {
            paymentSearchRepository.deleteById(event.getDocumentId());
            log.debug("Deleted payment from index: {}", sanitize.sanitizeLogging(event.getDocumentId()));
        } else {
            PaymentSearchIndex payment = deserialize(event.getPayload(), PaymentSearchIndex.class);
            paymentSearchRepository.save(payment);
            log.debug("Indexed payment: {}", sanitize.sanitizeLogging(event.getDocumentId()));
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return jsonMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Failed to deserialize JSON to {}: {}", clazz.getSimpleName(), e.getMessage());
            throw new RuntimeException("Failed to deserialize Elasticsearch document", e);
        }
    }
}
