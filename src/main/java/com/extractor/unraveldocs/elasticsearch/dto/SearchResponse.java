package com.extractor.unraveldocs.elasticsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic search response DTO for Elasticsearch queries.
 *
 * @param <T> The type of search results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse<T> {

    /**
     * List of search results.
     */
    @Builder.Default
    private List<T> results = new ArrayList<>();

    /**
     * Total number of matching documents.
     */
    private Long totalHits;

    /**
     * Current page number.
     */
    private Integer page;

    /**
     * Page size.
     */
    private Integer size;

    /**
     * Total number of pages.
     */
    private Integer totalPages;

    /**
     * Time taken to execute the search (in milliseconds).
     */
    private Long took;

    /**
     * Highlighted text snippets for each result (by document ID).
     */
    @Builder.Default
    private Map<String, List<String>> highlights = new HashMap<>();

    /**
     * Facet counts for aggregations.
     */
    @Builder.Default
    private Map<String, Map<String, Long>> facets = new HashMap<>();

    /**
     * Whether there are more pages available.
     */
    public boolean hasNext() {
        return page < totalPages - 1;
    }

    /**
     * Whether there is a previous page.
     */
    public boolean hasPrevious() {
        return page > 0;
    }

    /**
     * Creates a response from a Spring Data Page object.
     */
    public static <T> SearchResponse<T> fromPage(org.springframework.data.domain.Page<T> page) {
        return SearchResponse.<T>builder()
                .results(page.getContent())
                .totalHits(page.getTotalElements())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }
}
