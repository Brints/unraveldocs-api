package com.extractor.unraveldocs.elasticsearch.events;

/**
 * Enum representing the type of document to be indexed in Elasticsearch.
 */
public enum IndexType {
    /**
     * Document index for OCR content and document metadata.
     */
    DOCUMENT,

    /**
     * User index for user search functionality.
     */
    USER,

    /**
     * Payment index for payment and receipt search.
     */
    PAYMENT,

    /**
     * Subscription index for subscription plan search.
     */
    SUBSCRIPTION
}
