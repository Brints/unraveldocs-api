package com.extractor.unraveldocs.elasticsearch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic search request DTO for Elasticsearch queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /**
     * Search query string.
     */
    private String query;

    /**
     * Page number (0-indexed).
     */
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    /**
     * Page size.
     */
    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 10;

    /**
     * Field to sort by.
     */
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * Sort direction (asc/desc).
     */
    @Builder.Default
    private String sortDirection = "desc";

    /**
     * Filter criteria as key-value pairs.
     */
    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();

    /**
     * Start date for date range filtering.
     */
    private OffsetDateTime dateFrom;

    /**
     * End date for date range filtering.
     */
    private OffsetDateTime dateTo;

    /**
     * Whether to include highlights in results.
     */
    @Builder.Default
    private Boolean includeHighlights = true;
}
