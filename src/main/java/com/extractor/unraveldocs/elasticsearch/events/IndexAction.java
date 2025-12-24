package com.extractor.unraveldocs.elasticsearch.events;

/**
 * Enum representing the type of indexing action to perform on Elasticsearch.
 */
public enum IndexAction {
    /**
     * Create a document in the index.
     */
    CREATE,

    /**
     * Update an existing document in the index.
     */
    UPDATE,

    /**
     * Delete a document from the index.
     */
    DELETE
}
