package com.extractor.unraveldocs.elasticsearch.service;

import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.elasticsearch.document.UserSearchIndex;
import com.extractor.unraveldocs.elasticsearch.dto.SearchRequest;
import com.extractor.unraveldocs.elasticsearch.dto.SearchResponse;
import com.extractor.unraveldocs.elasticsearch.events.ElasticsearchIndexEvent;
import com.extractor.unraveldocs.elasticsearch.events.IndexType;
import com.extractor.unraveldocs.elasticsearch.publisher.ElasticsearchEventPublisher;
import com.extractor.unraveldocs.elasticsearch.repository.UserSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Service for user search operations using Elasticsearch.
 * Used by admin dashboard for fast user lookups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
public class UserSearchService {

    private final UserSearchRepository userSearchRepository;
    private final ElasticsearchEventPublisher eventPublisher;
    private final SanitizeLogging sanitizer;

    /**
     * Searches users with optional filters.
     *
     * @param request The search request
     * @return Search response with matching users
     */
    public SearchResponse<UserSearchIndex> searchUsers(SearchRequest request) {
        log.debug("Searching users: query='{}'", sanitizer.sanitizeLogging(request.getQuery()));

        Pageable pageable = createPageable(request);
        Page<UserSearchIndex> page;

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            // Check for role filter
            if (request.getFilters().containsKey("role")) {
                String role = request.getFilters().get("role").toString();
                page = userSearchRepository.searchUsersByRole(request.getQuery(), role, pageable);
            }
            // Check for active status filter
            else if (request.getFilters().containsKey("isActive")) {
                Boolean isActive = Boolean.valueOf(request.getFilters().get("isActive").toString());
                page = userSearchRepository.searchUsersByActiveStatus(request.getQuery(), isActive, pageable);
            } else {
                page = userSearchRepository.searchUsers(request.getQuery(), pageable);
            }
        } else {
            // No query - apply filters directly
            if (request.getFilters().containsKey("role")) {
                String role = request.getFilters().get("role").toString();
                page = userSearchRepository.findByRole(role, pageable);
            } else if (request.getFilters().containsKey("isActive")) {
                Boolean isActive = Boolean.valueOf(request.getFilters().get("isActive").toString());
                page = userSearchRepository.findByIsActive(isActive, pageable);
            } else if (request.getFilters().containsKey("country")) {
                String country = request.getFilters().get("country").toString();
                page = userSearchRepository.findByCountry(country, pageable);
            } else {
                page = userSearchRepository.findAll(pageable);
            }
        }

        return SearchResponse.fromPage(page);
    }

    /**
     * Finds users by role.
     *
     * @param role     The role to filter by
     * @param pageable Pagination parameters
     * @return Page of users
     */
    public Page<UserSearchIndex> findByRole(String role, Pageable pageable) {
        return userSearchRepository.findByRole(role, pageable);
    }

    /**
     * Finds users by active status.
     *
     * @param isActive The active status
     * @param pageable Pagination parameters
     * @return Page of users
     */
    public Page<UserSearchIndex> findByActiveStatus(Boolean isActive, Pageable pageable) {
        return userSearchRepository.findByIsActive(isActive, pageable);
    }

    /**
     * Finds users by verification status.
     *
     * @param isVerified The verification status
     * @param pageable   Pagination parameters
     * @return Page of users
     */
    public Page<UserSearchIndex> findByVerifiedStatus(Boolean isVerified, Pageable pageable) {
        return userSearchRepository.findByIsVerified(isVerified, pageable);
    }

    /**
     * Indexes a user for search.
     *
     * @param user The user to index
     */
    public void indexUser(UserSearchIndex user) {
        log.debug("Indexing user: {}", sanitizer.sanitizeLogging(user.getId()));

        String payload = eventPublisher.toJsonPayload(user);
        ElasticsearchIndexEvent event = ElasticsearchIndexEvent.createEvent(
                user.getId(),
                IndexType.USER,
                payload);
        eventPublisher.publishUserIndexEvent(event);
    }

    /**
     * Indexes a user synchronously (for bulk operations).
     *
     * @param user The user to index
     */
    public void indexUserSync(UserSearchIndex user) {
        log.debug("Indexing user synchronously: {}", sanitizer.sanitizeLogging(user.getId()));
        userSearchRepository.save(user);
    }

    /**
     * Updates a user in the search index.
     *
     * @param user The user to update
     */
    public void updateUser(UserSearchIndex user) {
        log.debug("Updating user in index: {}", user.getId());

        String payload = eventPublisher.toJsonPayload(user);
        ElasticsearchIndexEvent event = ElasticsearchIndexEvent.updateEvent(
                user.getId(),
                IndexType.USER,
                payload);
        eventPublisher.publishUserIndexEvent(event);
    }

    /**
     * Deletes a user from the search index.
     *
     * @param userId The user ID to delete
     */
    public void deleteUser(String userId) {
        log.debug("Deleting user from index: {}", sanitizer.sanitizeLogging(userId));

        ElasticsearchIndexEvent event = ElasticsearchIndexEvent.deleteEvent(
                userId,
                IndexType.USER);
        eventPublisher.publishUserIndexEvent(event);
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
