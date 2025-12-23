package com.extractor.unraveldocs.elasticsearch.repository;

import com.extractor.unraveldocs.elasticsearch.document.DocumentSearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch repository for document search operations.
 */
@Repository
public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentSearchIndex, String> {

    /**
     * Find documents by user ID.
     */
    Page<DocumentSearchIndex> findByUserId(String userId, Pageable pageable);

    /**
     * Find documents by user ID and status.
     */
    Page<DocumentSearchIndex> findByUserIdAndStatus(String userId, String status, Pageable pageable);

    /**
     * Find documents by user ID and file type.
     */
    Page<DocumentSearchIndex> findByUserIdAndFileType(String userId, String fileType, Pageable pageable);

    /**
     * Find documents containing extracted text matching the query.
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {"term": {"userId": "?0"}},
                  {"match": {"extractedText": "?1"}}
                ]
              }
            }
            """)
    Page<DocumentSearchIndex> searchByUserIdAndExtractedText(String userId, String query, Pageable pageable);

    /**
     * Full-text search across multiple fields.
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {"term": {"userId": "?0"}}
                ],
                "should": [
                  {"match": {"extractedText": {"query": "?1", "boost": 2}}},
                  {"match": {"fileName": {"query": "?1", "boost": 1.5}}},
                  {"wildcard": {"fileName.keyword": {"value": "*?1*", "boost": 1}}}
                ],
                "minimum_should_match": 1
              }
            }
            """)
    Page<DocumentSearchIndex> searchDocuments(String userId, String query, Pageable pageable);

    /**
     * Find all documents by user ID ordered by creation date.
     */
    List<DocumentSearchIndex> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Delete all documents by user ID.
     */
    void deleteByUserId(String userId);

    /**
     * Delete all documents by collection ID.
     */
    void deleteByCollectionId(String collectionId);
}
