package com.extractor.unraveldocs.elasticsearch.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Event class for Elasticsearch indexing operations.
 * Published to RabbitMQ for asynchronous processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElasticsearchIndexEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier of the document to index.
     */
    private String documentId;

    /**
     * Type of index operation (CREATE, UPDATE, DELETE).
     */
    private IndexAction action;

    /**
     * Type of document being indexed.
     */
    private IndexType indexType;

    /**
     * Timestamp when the event was created.
     */
    private OffsetDateTime timestamp;

    /**
     * JSON payload containing the document data.
     * Used for CREATE and UPDATE operations.
     */
    private String payload;

    /**
     * Creates a new CREATE event.
     */
    public static ElasticsearchIndexEvent createEvent(String documentId, IndexType indexType, String payload) {
        return ElasticsearchIndexEvent.builder()
                .documentId(documentId)
                .action(IndexAction.CREATE)
                .indexType(indexType)
                .payload(payload)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    /**
     * Creates a new UPDATE event.
     */
    public static ElasticsearchIndexEvent updateEvent(String documentId, IndexType indexType, String payload) {
        return ElasticsearchIndexEvent.builder()
                .documentId(documentId)
                .action(IndexAction.UPDATE)
                .indexType(indexType)
                .payload(payload)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    /**
     * Creates a new DELETE event.
     */
    public static ElasticsearchIndexEvent deleteEvent(String documentId, IndexType indexType) {
        return ElasticsearchIndexEvent.builder()
                .documentId(documentId)
                .action(IndexAction.DELETE)
                .indexType(indexType)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
