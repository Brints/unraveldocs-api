package com.extractor.unraveldocs.elasticsearch.repository;

import com.extractor.unraveldocs.elasticsearch.document.UserSearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for user search operations.
 * Used by admin dashboard for fast user lookups.
 */
@Repository
public interface UserSearchRepository extends ElasticsearchRepository<UserSearchIndex, String> {

    /**
     * Find users by email.
     */
    Page<UserSearchIndex> findByEmail(String email, Pageable pageable);

    /**
     * Find users by role.
     */
    Page<UserSearchIndex> findByRole(String role, Pageable pageable);

    /**
     * Find users by active status.
     */
    Page<UserSearchIndex> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * Find users by verified status.
     */
    Page<UserSearchIndex> findByIsVerified(Boolean isVerified, Pageable pageable);

    /**
     * Find users by country.
     */
    Page<UserSearchIndex> findByCountry(String country, Pageable pageable);

    /**
     * Multi-field search across user data.
     */
    @Query("""
            {
              "bool": {
                "should": [
                  {"match": {"firstName": {"query": "?0", "boost": 2}}},
                  {"match": {"lastName": {"query": "?0", "boost": 2}}},
                  {"wildcard": {"email": {"value": "*?0*", "boost": 1.5}}},
                  {"match": {"organization": {"query": "?0", "boost": 1}}},
                  {"match": {"profession": {"query": "?0", "boost": 1}}}
                ],
                "minimum_should_match": 1
              }
            }
            """)
    Page<UserSearchIndex> searchUsers(String query, Pageable pageable);

    /**
     * Search users with role filter.
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {"term": {"role": "?1"}}
                ],
                "should": [
                  {"match": {"firstName": {"query": "?0", "boost": 2}}},
                  {"match": {"lastName": {"query": "?0", "boost": 2}}},
                  {"wildcard": {"email": {"value": "*?0*", "boost": 1.5}}}
                ],
                "minimum_should_match": 1
              }
            }
            """)
    Page<UserSearchIndex> searchUsersByRole(String query, String role, Pageable pageable);

    /**
     * Search users with active status filter.
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {"term": {"isActive": ?1}}
                ],
                "should": [
                  {"match": {"firstName": {"query": "?0", "boost": 2}}},
                  {"match": {"lastName": {"query": "?0", "boost": 2}}},
                  {"wildcard": {"email": {"value": "*?0*", "boost": 1.5}}}
                ],
                "minimum_should_match": 1
              }
            }
            """)
    Page<UserSearchIndex> searchUsersByActiveStatus(String query, Boolean isActive, Pageable pageable);
}
