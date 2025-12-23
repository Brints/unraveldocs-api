package com.extractor.unraveldocs.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch configuration for the UnravelDocs application.
 * Provides client beans and repository configuration.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
@EnableElasticsearchRepositories(basePackages = "com.extractor.unraveldocs.elasticsearch.repository")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    /**
     * Creates a low-level REST client for Elasticsearch.
     */
    @Bean
    public RestClient restClient() {
        String host = extractHost(elasticsearchUri);
        int port = extractPort(elasticsearchUri);
        String scheme = extractScheme(elasticsearchUri);

        log.info("Configuring Elasticsearch connection to {}://{}:{}", scheme, host, port);

        return RestClient.builder(new HttpHost(host, port, scheme)).build();
    }

    /**
     * Creates the Elasticsearch transport layer.
     */
    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    /**
     * Creates the Elasticsearch client for operations.
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    /**
     * Creates the ElasticsearchOperations bean for Spring Data Elasticsearch.
     */
    @Bean
    public ElasticsearchOperations elasticsearchTemplate(ElasticsearchClient client) {
        return new ElasticsearchTemplate(client);
    }

    private String extractHost(String uri) {
        String withoutScheme = uri.replaceFirst("https?://", "");
        if (withoutScheme.contains(":")) {
            return withoutScheme.substring(0, withoutScheme.indexOf(":"));
        }
        return withoutScheme;
    }

    private int extractPort(String uri) {
        String withoutScheme = uri.replaceFirst("https?://", "");
        if (withoutScheme.contains(":")) {
            String portStr = withoutScheme.substring(withoutScheme.indexOf(":") + 1);
            return Integer.parseInt(portStr);
        }
        return 9200; // default port
    }

    private String extractScheme(String uri) {
        if (uri.startsWith("https://")) {
            return "https";
        }
        return "http";
    }
}
