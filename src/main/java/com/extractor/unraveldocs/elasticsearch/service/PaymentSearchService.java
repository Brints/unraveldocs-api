package com.extractor.unraveldocs.elasticsearch.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.elasticsearch.document.PaymentSearchIndex;
import com.extractor.unraveldocs.elasticsearch.dto.SearchRequest;
import com.extractor.unraveldocs.elasticsearch.dto.SearchResponse;
import com.extractor.unraveldocs.elasticsearch.events.ElasticsearchIndexEvent;
import com.extractor.unraveldocs.elasticsearch.events.IndexType;
import com.extractor.unraveldocs.elasticsearch.publisher.ElasticsearchEventPublisher;
import com.extractor.unraveldocs.elasticsearch.repository.PaymentSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Service for payment search operations using Elasticsearch.
 * Used for admin payment oversight and user payment history.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class PaymentSearchService {

    private final PaymentSearchRepository paymentSearchRepository;
    private final ElasticsearchEventPublisher eventPublisher;
    private final SanitizeLogging sanitizer;

    /**
     * Searches payments with optional filters.
     *
     * @param request The search request
     * @return Search response with matching payments
     */
    public SearchResponse<PaymentSearchIndex> searchPayments(SearchRequest request) {
        log.debug("Searching payments: query='{}'", sanitizer.sanitizeLogging(request.getQuery()));

        Pageable pageable = createPageable(request);
        Page<PaymentSearchIndex> page;

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            // Check for provider filter
            if (request.getFilters().containsKey("paymentProvider")) {
                String provider = request.getFilters().get("paymentProvider").toString();
                page = paymentSearchRepository.searchPaymentsByProvider(request.getQuery(), provider, pageable);
            }
            // Check for status filter
            else if (request.getFilters().containsKey("status")) {
                String status = request.getFilters().get("status").toString();
                page = paymentSearchRepository.searchPaymentsByStatus(request.getQuery(), status, pageable);
            } else {
                page = paymentSearchRepository.searchPayments(request.getQuery(), pageable);
            }
        } else {
            // No query - apply filters directly
            if (request.getFilters().containsKey("paymentProvider")) {
                String provider = request.getFilters().get("paymentProvider").toString();
                page = paymentSearchRepository.findByPaymentProvider(provider, pageable);
            } else if (request.getFilters().containsKey("status")) {
                String status = request.getFilters().get("status").toString();
                page = paymentSearchRepository.findByStatus(status, pageable);
            } else if (request.getFilters().containsKey("currency")) {
                String currency = request.getFilters().get("currency").toString();
                page = paymentSearchRepository.findByCurrency(currency, pageable);
            } else if (request.getDateFrom() != null && request.getDateTo() != null) {
                page = paymentSearchRepository.findByPaidAtBetween(
                        request.getDateFrom(), request.getDateTo(), pageable);
            } else {
                page = paymentSearchRepository.findAll(pageable);
            }
        }

        return SearchResponse.fromPage(page);
    }

    /**
     * Gets payments for a specific user.
     *
     * @param userId   The user ID
     * @param pageable Pagination parameters
     * @return Page of payments
     */
    public Page<PaymentSearchIndex> getPaymentsByUserId(String userId, Pageable pageable) {
        return paymentSearchRepository.findByUserId(userId, pageable);
    }

    /**
     * Finds a payment by receipt number.
     *
     * @param receiptNumber The receipt number
     * @param pageable      Pagination parameters
     * @return Page of payments (usually 0 or 1 result)
     */
    public Page<PaymentSearchIndex> findByReceiptNumber(String receiptNumber, Pageable pageable) {
        return paymentSearchRepository.findByReceiptNumber(receiptNumber, pageable);
    }

    /**
     * Indexes a payment for search.
     *
     * @param payment The payment to index
     */
    public void indexPayment(PaymentSearchIndex payment) {
        log.debug("Indexing payment: {}", sanitizer.sanitizeLogging(payment.getId()));

        String payload = eventPublisher.toJsonPayload(payment);
        ElasticsearchIndexEvent event = ElasticsearchIndexEvent.createEvent(
                payment.getId(),
                IndexType.PAYMENT,
                payload);
        eventPublisher.publishPaymentIndexEvent(event);
    }

    /**
     * Indexes a payment synchronously (for bulk operations).
     *
     * @param payment The payment to index
     */
    public void indexPaymentSync(PaymentSearchIndex payment) {
        log.debug("Indexing payment synchronously: {}", sanitizer.sanitizeLogging(payment.getId()));
        paymentSearchRepository.save(payment);
    }

    /**
     * Deletes a payment from the search index.
     *
     * @param paymentId The payment ID to delete
     */
    public void deletePayment(String paymentId) {
        log.debug("Deleting payment from index: {}", sanitizer.sanitizeLogging(paymentId));

        ElasticsearchIndexEvent event = ElasticsearchIndexEvent.deleteEvent(
                paymentId,
                IndexType.PAYMENT);
        eventPublisher.publishPaymentIndexEvent(event);
    }

    /**
     * Deletes all payments for a user from the search index.
     *
     * @param userId The user ID
     */
    public void deletePaymentsByUserId(String userId) {
        log.info("Deleting all payments for user: {}", sanitizer.sanitizeLogging(userId));
        paymentSearchRepository.deleteByUserId(userId);
    }

    private Pageable createPageable(SearchRequest request) {
        Sort sort = Sort.by(
                request.getSortDirection().equalsIgnoreCase("asc")
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC,
                request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
