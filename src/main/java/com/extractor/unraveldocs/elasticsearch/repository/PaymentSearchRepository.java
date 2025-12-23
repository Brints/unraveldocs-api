package com.extractor.unraveldocs.elasticsearch.repository;

import com.extractor.unraveldocs.elasticsearch.document.PaymentSearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * Elasticsearch repository for payment search operations.
 * Used for admin payment oversight and user payment history.
 */
@Repository
public interface PaymentSearchRepository extends ElasticsearchRepository<PaymentSearchIndex, String> {

    /**
     * Find payments by user ID.
     */
    Page<PaymentSearchIndex> findByUserId(String userId, Pageable pageable);

    /**
     * Find payments by receipt number.
     */
    Page<PaymentSearchIndex> findByReceiptNumber(String receiptNumber, Pageable pageable);

    /**
     * Find payments by payment provider.
     */
    Page<PaymentSearchIndex> findByPaymentProvider(String paymentProvider, Pageable pageable);

    /**
     * Find payments by status.
     */
    Page<PaymentSearchIndex> findByStatus(String status, Pageable pageable);

    /**
     * Find payments by currency.
     */
    Page<PaymentSearchIndex> findByCurrency(String currency, Pageable pageable);

    /**
     * Find payments within a date range.
     */
    Page<PaymentSearchIndex> findByPaidAtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    /**
     * Multi-field search across payment data.
     */
    @Query("""
            {
              "bool": {
                "should": [
                  {"term": {"receiptNumber": "?0"}},
                  {"wildcard": {"userEmail": {"value": "*?0*"}}},
                  {"match": {"userName": "?0"}},
                  {"match": {"description": "?0"}},
                  {"term": {"externalPaymentId": "?0"}}
                ],
                "minimum_should_match": 1
              }
            }
            """)
    Page<PaymentSearchIndex> searchPayments(String query, Pageable pageable);

    /**
     * Search payments with provider filter.
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {"term": {"paymentProvider": "?1"}}
                ],
                "should": [
                  {"term": {"receiptNumber": "?0"}},
                  {"wildcard": {"userEmail": {"value": "*?0*"}}},
                  {"match": {"userName": "?0"}}
                ],
                "minimum_should_match": 1
              }
            }
            """)
    Page<PaymentSearchIndex> searchPaymentsByProvider(String query, String provider, Pageable pageable);

    /**
     * Search payments with status filter.
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {"term": {"status": "?1"}}
                ],
                "should": [
                  {"term": {"receiptNumber": "?0"}},
                  {"wildcard": {"userEmail": {"value": "*?0*"}}},
                  {"match": {"userName": "?0"}}
                ],
                "minimum_should_match": 1
              }
            }
            """)
    Page<PaymentSearchIndex> searchPaymentsByStatus(String query, String status, Pageable pageable);

    /**
     * Delete all payments by user ID.
     */
    void deleteByUserId(String userId);
}
